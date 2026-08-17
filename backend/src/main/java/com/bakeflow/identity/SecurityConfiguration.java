package com.bakeflow.identity;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
    }

    @Bean
    JwtEncoder jwtEncoder(@Value("${security.jwt-secret}") String secret) {
        validateSecret(secret);
        return new NimbusJwtEncoder(new ImmutableSecret<>(key(secret)));
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${security.jwt-secret}") String secret) {
        validateSecret(secret);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key(secret)).build();
        OAuth2TokenValidator<Jwt> subjectValidator = jwt -> {
            try {
                UUID.fromString(jwt.getSubject());
                return OAuth2TokenValidatorResult.success();
            } catch (Exception exception) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "A valid subject claim is required", null));
            }
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), subjectValidator));
        return decoder;
    }

    @Bean
    CorsConfigurationSource cors(@Value("${security.allowed-origins}") List<String> origins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-ID"));
        configuration.setExposedHeaders(List.of("X-Request-ID"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    SecurityFilterChain filter(HttpSecurity http, SecurityErrorResponse errors,
            @Value("${security.permit-all:false}") boolean permitAll) throws Exception {
        if (permitAll) {
            return http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
        }
        return http.csrf(csrf -> csrf.disable())
                .headers(headers -> headers.referrerPolicy(policy ->
                        policy.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .cors(cors -> {})
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                errors.write(request, response, 401, "UNAUTHENTICATED"))
                        .accessDeniedHandler((request, response, exception) ->
                                errors.write(request, response, 403, "ACCESS_DENIED")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh",
                                "/api/system/health/**", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/v1/users/**").hasAuthority("SCOPE_USER_READ")
                        .requestMatchers(HttpMethod.GET, "/api/v1/items/**", "/api/v1/batches/**",
                                "/api/v1/locations/**").hasAuthority("SCOPE_ITEM_READ")
                        .requestMatchers("/api/v1/items/**", "/api/v1/batches/**", "/api/v1/locations/**")
                                .hasAuthority("SCOPE_ITEM_WRITE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/recipes/**")
                                .hasAuthority("SCOPE_RECIPE_READ")
                        .requestMatchers("/api/v1/recipes/**").hasAuthority("SCOPE_RECIPE_WRITE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/production-orders/**",
                                "/api/v1/production-dashboard/**").hasAuthority("SCOPE_PRODUCTION_READ")
                        .requestMatchers(HttpMethod.POST, "/api/v1/production-orders/*/start")
                                .hasAuthority("SCOPE_PRODUCTION_START")
                        .requestMatchers(HttpMethod.POST, "/api/v1/production-orders/*/complete")
                                .hasAuthority("SCOPE_PRODUCTION_COMPLETE")
                        .requestMatchers(HttpMethod.POST, "/api/v1/production-orders/*/cancel")
                                .hasAuthority("SCOPE_PRODUCTION_CANCEL")
                        .requestMatchers(HttpMethod.POST, "/api/v1/production-orders/**")
                                .hasAuthority("SCOPE_PRODUCTION_CREATE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/stock/balances")
                                .hasAuthority("SCOPE_STOCK_READ")
                        .requestMatchers(HttpMethod.GET, "/api/v1/stock/movements/**")
                                .hasAuthority("SCOPE_MOVEMENT_READ")
                        .requestMatchers(HttpMethod.POST, "/api/v1/stock/entries")
                                .hasAuthority("SCOPE_STOCK_ENTRY")
                        .requestMatchers(HttpMethod.POST, "/api/v1/stock/exits")
                                .hasAuthority("SCOPE_STOCK_EXIT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/stock/transfers")
                                .hasAuthority("SCOPE_STOCK_TRANSFER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/stock/losses")
                                .hasAuthority("SCOPE_STOCK_LOSS")
                        .requestMatchers(HttpMethod.POST, "/api/v1/stock/adjustments")
                                .hasAuthority("SCOPE_STOCK_ADJUSTMENT")
                        .requestMatchers("/api/v1/integrations/**", "/api/v1/system/**", "/api/system/status")
                                .authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> {}))
                .build();
    }

    private static SecretKeySpec key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static void validateSecret(String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT secret must contain at least 32 characters");
        }
    }
}
