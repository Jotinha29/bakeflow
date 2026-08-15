package com.bakeflow.identity;

import static com.bakeflow.identity.IdentityDtos.*;

import com.bakeflow.audit.AuditService;
import com.bakeflow.integration.application.CacheService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IdentityService {
  private final JdbcTemplate jdbc;
  private final PasswordEncoder passwords;
  private final JwtEncoder jwt;
  private final CacheService cache;
  private final AuditService audit;
  private final Duration accessTtl, refreshTtl;
  private final int loginLimit;
  private final SecureRandom random = new SecureRandom();

  public IdentityService(
      JdbcTemplate jdbc,
      PasswordEncoder passwords,
      JwtEncoder jwt,
      CacheService cache,
      AuditService audit,
      @Value("${security.access-token-ttl}") Duration accessTtl,
      @Value("${security.refresh-token-ttl}") Duration refreshTtl,
      @Value("${security.login-rate-limit}") int loginLimit) {
    this.jdbc = jdbc;
    this.passwords = passwords;
    this.jwt = jwt;
    this.cache = cache;
    this.audit = audit;
    this.accessTtl = accessTtl;
    this.refreshTtl = refreshTtl;
    this.loginLimit = loginLimit;
  }

  public TokenPair login(LoginInput in, String client, String agent) {
    String email = email(in.email());
    if (!cache.incrementWithinLimit(
        "bakeflow:ratelimit:login:" + client + ":" + sha(email).substring(0, 16),
        loginLimit,
        Duration.ofMinutes(5))) throw new IdentityException("RATE_LIMIT_EXCEEDED");
    var row = userByEmail(email);
    if (row == null
        || !row.active
        || in.password() == null
        || !passwords.matches(in.password(), row.hash)) {
      audit.recordIndependent(
          "USER_LOGIN_FAILED",
          "USER",
          row == null ? null : row.id,
          "Login failed",
          Map.of("emailHash", sha(email)));
      throw new IdentityException("INVALID_CREDENTIALS");
    }
    Instant now = Instant.now();
    jdbc.update(
        "UPDATE users SET last_login_at=?,updated_at=? WHERE id=?",
        Timestamp.from(now),
        Timestamp.from(now),
        row.id);
    audit.record("USER_LOGIN_SUCCEEDED", "USER", row.id, "User logged in", Map.of());
    return tokens(row.id, UUID.randomUUID(), agent, null);
  }

  @Transactional(noRollbackFor = IdentityException.class)
  public TokenPair refresh(String raw, String agent) {
    if (raw == null) throw new IdentityException("INVALID_REFRESH_TOKEN");
    String hash = sha(raw);
    List<TokenRow> rows =
        jdbc.query("SELECT * FROM refresh_tokens WHERE token_hash=? FOR UPDATE", this::token, hash);
    if (rows.isEmpty()) throw new IdentityException("INVALID_REFRESH_TOKEN");
    TokenRow old = rows.getFirst();
    if (old.revokedAt != null) {
      jdbc.update(
          "UPDATE refresh_tokens SET revoked_at=COALESCE(revoked_at,?) WHERE family_id=?",
          Timestamp.from(Instant.now()),
          old.familyId);
      throw new IdentityException("REFRESH_TOKEN_REUSED");
    }
    if (old.expiresAt.isBefore(Instant.now()) || !active(old.userId))
      throw new IdentityException("INVALID_REFRESH_TOKEN");
    return tokens(old.userId, old.familyId, agent, old.id);
  }

  public void logout(String raw) {
    if (raw != null)
      jdbc.update(
          "UPDATE refresh_tokens SET revoked_at=COALESCE(revoked_at,?) WHERE token_hash=?",
          Timestamp.from(Instant.now()),
          sha(raw));
    UUID id = SecuritySupport.currentUserId();
    audit.record("USER_LOGOUT", "USER", id, "User logged out", Map.of());
  }

  public void logoutAll(UUID user) {
    jdbc.update(
        "UPDATE refresh_tokens SET revoked_at=COALESCE(revoked_at,?) WHERE user_id=?",
        Timestamp.from(Instant.now()),
        user);
    audit.record("USER_LOGOUT", "USER", user, "All user sessions revoked", Map.of());
  }

  @Transactional(readOnly = true)
  public UserView me(UUID id) {
    return view(id);
  }

  @Transactional(readOnly = true)
  public List<SessionView> sessions(UUID user, String currentHash) {
    return jdbc.query(
        "SELECT * FROM refresh_tokens WHERE user_id=? AND revoked_at IS NULL AND"
            + " expires_at>CURRENT_TIMESTAMP ORDER BY created_at DESC",
        (r, n) ->
            new SessionView(
                r.getObject("id", UUID.class),
                instant(r, "created_at"),
                instant(r, "last_used_at"),
                instant(r, "expires_at"),
                r.getString("user_agent"),
                Objects.equals(r.getString("token_hash"), currentHash)),
        user);
  }

  public void revokeSession(UUID user, UUID session) {
    int changed =
        jdbc.update(
            "UPDATE refresh_tokens SET revoked_at=? WHERE id=? AND user_id=? AND revoked_at IS"
                + " NULL",
            Timestamp.from(Instant.now()),
            session,
            user);
    if (changed == 0) throw new IdentityException("SESSION_NOT_FOUND");
  }

  public void changePassword(UUID user, PasswordChange in, String currentRefresh) {
    validatePassword(in.newPassword());
    UserRow row = user(user);
    if (!passwords.matches(in.currentPassword(), row.hash))
      throw new IdentityException("INVALID_CREDENTIALS");
    jdbc.update(
        "UPDATE users SET password_hash=?,updated_at=? WHERE id=?",
        passwords.encode(in.newPassword()),
        Timestamp.from(Instant.now()),
        user);
    jdbc.update(
        "UPDATE refresh_tokens SET revoked_at=COALESCE(revoked_at,?) WHERE user_id=? AND"
            + " token_hash<>?",
        Timestamp.from(Instant.now()),
        user,
        currentRefresh == null ? "" : sha(currentRefresh));
    audit.record("USER_PASSWORD_CHANGED", "USER", user, "Password changed", Map.of());
  }

  @Transactional(readOnly = true)
  public UserPage users(String search, Boolean active, String role, int page, int size) {
    int safeSize = Math.min(Math.max(size, 1), 100), offset = Math.max(page, 0) * safeSize;
    String q = search == null ? "" : search.trim().toLowerCase(),
        roleFilter = role == null ? "" : role;
    List<UUID> ids =
        jdbc.query(
            "SELECT DISTINCT u.id,u.name FROM users u LEFT JOIN user_roles ur ON ur.user_id=u.id"
                + " LEFT JOIN roles r ON r.id=ur.role_id WHERE (?='' OR LOWER(u.name) LIKE ? OR"
                + " u.email LIKE ?) AND (CAST(? AS BOOLEAN) IS NULL OR u.active=?) AND (?='' OR"
                + " r.code=?) ORDER BY u.name LIMIT ? OFFSET ?",
            (r, n) -> r.getObject(1, UUID.class),
            q,
            "%" + q + "%",
            "%" + q + "%",
            active,
            active,
            roleFilter,
            roleFilter,
            safeSize,
            offset);
    Long total =
        jdbc.queryForObject(
            "SELECT COUNT(DISTINCT u.id) FROM users u LEFT JOIN user_roles ur ON ur.user_id=u.id"
                + " LEFT JOIN roles r ON r.id=ur.role_id WHERE (?='' OR LOWER(u.name) LIKE ? OR"
                + " u.email LIKE ?) AND (CAST(? AS BOOLEAN) IS NULL OR u.active=?) AND (?='' OR"
                + " r.code=?)",
            Long.class,
            q,
            "%" + q + "%",
            "%" + q + "%",
            active,
            active,
            roleFilter,
            roleFilter);
    return new UserPage(
        ids.stream().map(this::view).toList(), total == null ? 0 : total, page, safeSize);
  }

  public UserView create(UserInput in) {
    validatePassword(in.password());
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    jdbc.update(
        "INSERT INTO users(id,name,email,password_hash,active,created_at,updated_at)"
            + " VALUES(?,?,?,?,?,?,?)",
        id,
        name(in.name()),
        email(in.email()),
        passwords.encode(in.password()),
        in.active(),
        Timestamp.from(now),
        Timestamp.from(now));
    roles(id, in.roles());
    audit.record("USER_CREATED", "USER", id, "User created", Map.of("email", email(in.email())));
    return view(id);
  }

  public UserView update(UUID id, UserUpdate in) {
    UserRow before = user(id);
    jdbc.update(
        "UPDATE users SET name=?,email=?,active=?,updated_at=? WHERE id=?",
        name(in.name()),
        email(in.email()),
        in.active(),
        Timestamp.from(Instant.now()),
        id);
    roles(id, in.roles());
    if (before.active && !in.active()) {
      jdbc.update(
          "UPDATE refresh_tokens SET revoked_at=COALESCE(revoked_at,?) WHERE user_id=?",
          Timestamp.from(Instant.now()),
          id);
      audit.record("USER_DEACTIVATED", "USER", id, "User deactivated", Map.of());
    } else audit.record("USER_UPDATED", "USER", id, "User updated", Map.of());
    return view(id);
  }

  public void ensureAdmin(String email, String password) {
    if (password == null || password.isBlank()) return;
    validatePassword(password);
    String normalized = email(email);
    Integer admin =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON"
                + " r.id=ur.role_id WHERE u.email=? AND r.code='ADMIN'",
            Integer.class,
            normalized);
    if (admin != null && admin > 0) return;
    UserRow existing = userByEmail(normalized);
    if (existing == null)
      create(new UserInput("Administrador BakeFlow", normalized, password, Set.of("ADMIN"), true));
    else {
      jdbc.update(
          "INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code='ADMIN' AND"
              + " NOT EXISTS(SELECT 1 FROM user_roles WHERE user_id=? AND role_id=roles.id)",
          existing.id,
          existing.id);
      audit.record(
          "USER_UPDATED", "USER", existing.id, "Demo administrator role ensured", Map.of());
    }
  }

  private TokenPair tokens(UUID user, UUID family, String agent, UUID replaced) {
    UserView view = view(user);
    Instant now = Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .subject(user.toString())
            .issuedAt(now)
            .expiresAt(now.plus(accessTtl))
            .id(UUID.randomUUID().toString())
            .claim("scope", String.join(" ", view.permissions()))
            .build();
    var header =
        org.springframework.security.oauth2.jwt.JwsHeader.with(
                org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
            .build();
    String access = jwt.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO"
            + " refresh_tokens(id,user_id,token_hash,family_id,expires_at,created_at,last_used_at,user_agent)"
            + " VALUES(?,?,?,?,?,?,?,?)",
        id,
        user,
        sha(raw),
        family,
        Timestamp.from(now.plus(refreshTtl)),
        Timestamp.from(now),
        Timestamp.from(now),
        agent == null ? null : agent.substring(0, Math.min(200, agent.length())));
    if (replaced != null)
      jdbc.update(
          "UPDATE refresh_tokens SET revoked_at=?,replaced_by_token_id=?,last_used_at=? WHERE id=?",
          Timestamp.from(now),
          id,
          Timestamp.from(now),
          replaced);
    return new TokenPair(new TokenResponse(access, view), raw, id);
  }

  private UserView view(UUID id) {
    UserRow u = user(id);
    Set<String> roles =
        new TreeSet<>(
            jdbc.queryForList(
                "SELECT r.code FROM roles r JOIN user_roles ur ON ur.role_id=r.id WHERE"
                    + " ur.user_id=?",
                String.class,
                id));
    Set<String> permissions =
        new TreeSet<>(
            jdbc.queryForList(
                "SELECT DISTINCT p.code FROM permissions p JOIN role_permissions rp ON"
                    + " rp.permission_id=p.id JOIN user_roles ur ON ur.role_id=rp.role_id WHERE"
                    + " ur.user_id=?",
                String.class,
                id));
    return new UserView(u.id, u.name, u.email, u.active, u.lastLogin, roles, permissions);
  }

  private void roles(UUID user, Set<String> codes) {
    if (codes == null || codes.isEmpty()) throw new IdentityException("ROLE_REQUIRED");
    jdbc.update("DELETE FROM user_roles WHERE user_id=?", user);
    for (String code : codes) {
      int n =
          jdbc.update(
              "INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code=?",
              user,
              code);
      if (n == 0) throw new IdentityException("INVALID_ROLE");
    }
  }

  private UserRow userByEmail(String e) {
    List<UserRow> v = jdbc.query("SELECT * FROM users WHERE email=?", this::mapUser, e);
    return v.isEmpty() ? null : v.getFirst();
  }

  private UserRow user(UUID id) {
    List<UserRow> v = jdbc.query("SELECT * FROM users WHERE id=?", this::mapUser, id);
    if (v.isEmpty()) throw new IdentityException("USER_NOT_FOUND");
    return v.getFirst();
  }

  private UserRow mapUser(ResultSet r, int n) throws SQLException {
    return new UserRow(
        r.getObject("id", UUID.class),
        r.getString("name"),
        r.getString("email"),
        r.getString("password_hash"),
        r.getBoolean("active"),
        instant(r, "last_login_at"));
  }

  private TokenRow token(ResultSet r, int n) throws SQLException {
    return new TokenRow(
        r.getObject("id", UUID.class),
        r.getObject("user_id", UUID.class),
        r.getObject("family_id", UUID.class),
        instant(r, "expires_at"),
        instant(r, "revoked_at"));
  }

  private boolean active(UUID id) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject("SELECT active FROM users WHERE id=?", Boolean.class, id));
  }

  private Instant instant(ResultSet r, String c) throws SQLException {
    Timestamp t = r.getTimestamp(c);
    return t == null ? null : t.toInstant();
  }

  private String email(String e) {
    if (e == null || e.length() > 254 || !e.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
      throw new IdentityException("INVALID_EMAIL");
    return e.trim().toLowerCase(Locale.ROOT);
  }

  private String name(String n) {
    if (n == null || n.isBlank() || n.length() > 160) throw new IdentityException("INVALID_NAME");
    return n.trim();
  }

  private void validatePassword(String p) {
    if (p == null || p.length() < 8 || p.length() > 128)
      throw new IdentityException("INVALID_PASSWORD_POLICY");
  }

  static String sha(String s) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  record TokenPair(TokenResponse response, String refreshToken, UUID sessionId) {}

  private record UserRow(
      UUID id, String name, String email, String hash, boolean active, Instant lastLogin) {}

  private record TokenRow(
      UUID id, UUID userId, UUID familyId, Instant expiresAt, Instant revokedAt) {}
}
