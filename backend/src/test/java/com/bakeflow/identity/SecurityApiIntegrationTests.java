package com.bakeflow.identity;

import static com.bakeflow.identity.IdentityDtos.LoginInput;
import static com.bakeflow.identity.IdentityDtos.UserInput;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bakeflow.BakeFlowBackendApplication;
import com.bakeflow.integration.application.CacheService;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = BakeFlowBackendApplication.class, properties = "security.permit-all=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityApiIntegrationTests {
    private static final String PASSWORD = "a secure test passphrase";
    @Autowired private MockMvc mvc;
    @Autowired private IdentityService identity;
    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private CacheService cache;

    @BeforeEach
    void allowLogin() {
        when(cache.incrementWithinLimit(anyString(), anyInt(), any())).thenReturn(true);
    }

    @Test
    void unauthenticatedRequestIs401AndHealthRemainsPublic() throws Exception {
        mvc.perform(get("/api/v1/items")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/system/health/live")).andExpect(status().isOk());
    }

    @Test
    void representativeRoleBoundariesReturn403OrReachTheBusinessEndpoint() throws Exception {
        String admin = token("ADMIN");
        String manager = token("MANAGER");
        String operator = token("OPERATOR");
        String viewer = token("VIEWER");

        mvc.perform(get("/api/v1/users").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/recipes").header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/production-orders/preview").header("Authorization", bearer(operator))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/items").header("Authorization", bearer(viewer)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/items").header("Authorization", bearer(viewer))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/users").header("Authorization", bearer(operator))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void malformedExpiredInvalidSignatureAndMissingSubjectTokensAre401() throws Exception {
        mvc.perform(get("/api/v1/items").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/items").header("Authorization", "Bearer aaa.bbb.ccc"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/items").header("Authorization", bearer(jwt(null, Instant.now().minusSeconds(30)))))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/items").header("Authorization", bearer(jwt(UUID.randomUUID(), Instant.now().minusSeconds(120)))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshCookieHasSecurityAttributesAndLogoutRevokesIt() throws Exception {
        String email = create("ADMIN");
        var login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("bakeflow_refresh", true))
                .andExpect(cookie().secure("bakeflow_refresh", false))
                .andExpect(cookie().path("bakeflow_refresh", "/api/v1/auth"))
                .andReturn();
        Cookie refresh = login.getResponse().getCookie("bakeflow_refresh");
        String access = identity.login(new LoginInput(email, PASSWORD), "test", "agent").response().accessToken();

        mvc.perform(post("/api/v1/auth/logout").header("Authorization", bearer(access)).cookie(refresh))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("bakeflow_refresh", 0));
        mvc.perform(post("/api/v1/auth/refresh").cookie(refresh)).andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotRevokeAnotherUsersSession() throws Exception {
        String emailA = create("VIEWER");
        String emailB = create("VIEWER");
        var loginA = identity.login(new LoginInput(emailA, PASSWORD), "a", "agent-a");
        var loginB = identity.login(new LoginInput(emailB, PASSWORD), "b", "agent-b");

        mvc.perform(delete("/api/v1/auth/sessions/{id}", loginB.sessionId())
                        .header("Authorization", bearer(loginA.response().accessToken())))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/auth/refresh").cookie(
                        new Cookie("bakeflow_refresh", loginB.refreshToken())))
                .andExpect(status().isOk());
    }

    @Test
    void loginRateLimitReturns429() throws Exception {
        when(cache.incrementWithinLimit(anyString(), anyInt(), any())).thenReturn(true, false);
        String email = create("VIEWER");
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(email, "incorrect password")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(email, "incorrect password")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void logoutAllRevokesEverySessionAndAdministrativeAuditIdentifiesTheActor() throws Exception {
        String adminEmail = create("ADMIN");
        var sessionA = identity.login(new LoginInput(adminEmail, PASSWORD), "a", "agent-a");
        var sessionB = identity.login(new LoginInput(adminEmail, PASSWORD), "b", "agent-b");
        String createdEmail = "audited-" + UUID.randomUUID() + "@bakeflow.local";
        mvc.perform(post("/api/v1/users").header("Authorization", bearer(sessionA.response().accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Audited User\",\"email\":\"" + createdEmail +
                                "\",\"password\":\"" + PASSWORD +
                                "\",\"roles\":[\"VIEWER\"],\"active\":true}"))
                .andExpect(status().isOk());
        UUID adminId = jdbc.queryForObject("SELECT id FROM users WHERE email=?", UUID.class, adminEmail);
        UUID actor = jdbc.queryForObject(
                "SELECT actor_user_id FROM audit_events WHERE event_type='USER_CREATED' AND actor_user_id=? ORDER BY occurred_at DESC LIMIT 1",
                UUID.class, adminId);
        org.assertj.core.api.Assertions.assertThat(actor).isEqualTo(adminId);

        mvc.perform(post("/api/v1/auth/logout-all")
                        .header("Authorization", bearer(sessionA.response().accessToken())))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/refresh").cookie(
                        new Cookie("bakeflow_refresh", sessionA.refreshToken())))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(
                        new Cookie("bakeflow_refresh", sessionB.refreshToken())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inactiveUserKeepsShortAccessTokenButCannotRefresh() throws Exception {
        String email = create("VIEWER");
        var login = identity.login(new LoginInput(email, PASSWORD), "test", "agent");
        UUID id = jdbc.queryForObject("SELECT id FROM users WHERE email=?", UUID.class, email);
        identity.update(id, new IdentityDtos.UserUpdate("Inactive Viewer", email, Set.of("VIEWER"), false));

        mvc.perform(get("/api/v1/items").header("Authorization", bearer(login.response().accessToken())))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/refresh").cookie(
                        new Cookie("bakeflow_refresh", login.refreshToken())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordChangeValidatesCurrentPasswordAndRevokesOtherSessions() throws Exception {
        String email = create("VIEWER");
        var current = identity.login(new LoginInput(email, PASSWORD), "current", "current-agent");
        var other = identity.login(new LoginInput(email, PASSWORD), "other", "other-agent");
        mvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", bearer(current.response().accessToken()))
                        .cookie(new Cookie("bakeflow_refresh", current.refreshToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong password\",\"newPassword\":\"new secure passphrase\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", bearer(current.response().accessToken()))
                        .cookie(new Cookie("bakeflow_refresh", current.refreshToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD +
                                "\",\"newPassword\":\"new secure passphrase\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/refresh").cookie(
                        new Cookie("bakeflow_refresh", other.refreshToken())))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(email, PASSWORD))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(email, "new secure passphrase"))).andExpect(status().isOk());
    }

    private String token(String role) {
        String email = create(role);
        return identity.login(new LoginInput(email, PASSWORD), "test", role).response().accessToken();
    }

    private String create(String role) {
        String email = role.toLowerCase() + "-" + UUID.randomUUID() + "@bakeflow.local";
        identity.create(new UserInput("Security " + role, email, PASSWORD, Set.of(role), true));
        return email;
    }

    private String jwt(UUID subject, Instant expiration) {
        Instant issuedAt = expiration.isBefore(Instant.now().minusSeconds(60))
                ? expiration.minusSeconds(60)
                : Instant.now().minusSeconds(60);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder().issuedAt(issuedAt)
                .expiresAt(expiration).id(UUID.randomUUID().toString()).claim("scope", "ITEM_READ");
        if (subject != null) claims.subject(subject.toString());
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
    }

    private static String bearer(String token) { return "Bearer " + token; }
    private static String json(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
