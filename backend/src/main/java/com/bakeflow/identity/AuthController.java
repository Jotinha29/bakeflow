package com.bakeflow.identity;

import static com.bakeflow.identity.IdentityDtos.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String COOKIE = "bakeflow_refresh";
    private final IdentityService service;
    private final boolean secure;
    private final Duration ttl;
    private final Set<String> origins;

    public AuthController(IdentityService service,
            @Value("${security.cookie-secure}") boolean secure,
            @Value("${security.refresh-token-ttl}") Duration ttl,
            @Value("${security.allowed-origins}") List<String> origins) {
        this.service = service;
        this.secure = secure;
        this.ttl = ttl;
        this.origins = Set.copyOf(origins);
    }

    @PostMapping("/login")
    ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginInput input, HttpServletRequest request) {
        return withCookie(service.login(input, request.getRemoteAddr(), request.getHeader("User-Agent")));
    }

    @PostMapping("/refresh")
    ResponseEntity<TokenResponse> refresh(HttpServletRequest request) {
        validateOrigin(request);
        return withCookie(service.refresh(cookie(request), request.getHeader("User-Agent")));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request) {
        validateOrigin(request);
        service.logout(cookie(request));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearCookie()).build();
    }

    @PostMapping("/logout-all")
    ResponseEntity<Void> logoutAll(HttpServletRequest request) {
        validateOrigin(request);
        service.logoutAll(requiredUser());
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearCookie()).build();
    }

    @GetMapping("/me") UserView me() { return service.me(requiredUser()); }

    @GetMapping("/sessions")
    List<SessionView> sessions(HttpServletRequest request) {
        String raw = cookie(request);
        return service.sessions(requiredUser(), raw == null ? null : IdentityService.sha(raw));
    }

    @DeleteMapping("/sessions/{id}")
    void revoke(@PathVariable UUID id) { service.revokeSession(requiredUser(), id); }

    @PostMapping("/change-password")
    void changePassword(@Valid @RequestBody PasswordChange input, HttpServletRequest request) {
        validateOrigin(request);
        service.changePassword(requiredUser(), input, cookie(request));
    }

    private ResponseEntity<TokenResponse> withCookie(IdentityService.TokenPair pair) {
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                ResponseCookie.from(COOKIE, pair.refreshToken()).httpOnly(true).secure(secure)
                        .sameSite("Strict").path("/api/v1/auth").maxAge(ttl).build().toString())
                .body(pair.response());
    }

    private String clearCookie() {
        return ResponseCookie.from(COOKIE, "").httpOnly(true).secure(secure).sameSite("Strict")
                .path("/api/v1/auth").maxAge(Duration.ZERO).build().toString();
    }

    private String cookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) if (COOKIE.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private UUID requiredUser() {
        UUID id = SecuritySupport.currentUserId();
        if (id == null) throw new IdentityException("UNAUTHENTICATED");
        return id;
    }

    private void validateOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origins.contains(origin)) throw new IdentityException("INVALID_ORIGIN");
    }
}
