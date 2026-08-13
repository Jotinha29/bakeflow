package com.bakeflow.identity;
import com.nimbusds.jose.jwk.source.ImmutableSecret;import java.nio.charset.StandardCharsets;import java.util.List;import javax.crypto.spec.SecretKeySpec;import org.springframework.beans.factory.annotation.Value;import org.springframework.context.annotation.*;import org.springframework.http.HttpMethod;import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;import org.springframework.security.config.annotation.web.builders.HttpSecurity;import org.springframework.security.config.http.SessionCreationPolicy;import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.security.oauth2.jwt.*;import org.springframework.security.web.SecurityFilterChain;import org.springframework.web.cors.*;
@Configuration @EnableMethodSecurity public class SecurityConfiguration{
 @Bean PasswordEncoder passwordEncoder(){return new Argon2PasswordEncoder(16,32,1,65536,3);}
 @Bean JwtEncoder jwtEncoder(@Value("${security.jwt-secret}")String secret){if(secret.length()<32)throw new IllegalStateException("JWT secret must contain at least 32 characters");return new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256")));}
 @Bean JwtDecoder jwtDecoder(@Value("${security.jwt-secret}")String secret){return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256")).build();}
 @Bean CorsConfigurationSource cors(@Value("${security.allowed-origins}")List<String> origins){var c=new CorsConfiguration();c.setAllowedOrigins(origins);c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));c.setAllowedHeaders(List.of("Authorization","Content-Type","X-Request-ID"));c.setExposedHeaders(List.of("X-Request-ID"));c.setAllowCredentials(true);var s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return s;}
 @Bean SecurityFilterChain filter(HttpSecurity h,@Value("${security.permit-all:false}")boolean permitAll)throws Exception{if(permitAll)return h.csrf(c->c.disable()).authorizeHttpRequests(a->a.anyRequest().permitAll()).build();return h.csrf(c->c.disable()).cors(c->{}).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(a->a
  .requestMatchers("/api/v1/auth/login","/api/v1/auth/refresh","/api/system/health/**","/actuator/health/**").permitAll()
  .requestMatchers("/api/v1/users/**").hasAuthority("SCOPE_USER_READ")
  .requestMatchers(HttpMethod.GET,"/api/v1/items/**","/api/v1/batches/**","/api/v1/locations/**").hasAuthority("SCOPE_ITEM_READ")
  .requestMatchers("/api/v1/items/**","/api/v1/batches/**","/api/v1/locations/**").hasAuthority("SCOPE_ITEM_WRITE")
  .requestMatchers(HttpMethod.GET,"/api/v1/recipes/**").hasAuthority("SCOPE_RECIPE_READ")
  .requestMatchers("/api/v1/recipes/**").hasAuthority("SCOPE_RECIPE_WRITE")
  .requestMatchers(HttpMethod.GET,"/api/v1/production-orders/**","/api/v1/production-dashboard/**").hasAuthority("SCOPE_PRODUCTION_READ")
  .requestMatchers(HttpMethod.POST,"/api/v1/production-orders/*/start").hasAuthority("SCOPE_PRODUCTION_START")
  .requestMatchers(HttpMethod.POST,"/api/v1/production-orders/*/complete").hasAuthority("SCOPE_PRODUCTION_COMPLETE")
  .requestMatchers(HttpMethod.POST,"/api/v1/production-orders/*/cancel").hasAuthority("SCOPE_PRODUCTION_CANCEL")
  .requestMatchers(HttpMethod.POST,"/api/v1/production-orders/**").hasAuthority("SCOPE_PRODUCTION_CREATE")
  .requestMatchers("/api/v1/integrations/**","/api/v1/system/**","/api/system/status").authenticated().anyRequest().authenticated())
  .oauth2ResourceServer(o->o.jwt(j->{})).build();}
}
