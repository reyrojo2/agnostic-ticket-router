package com.agnosticrouter.decision.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean JwtDecoder jwtDecoder(@Value("${app.security.jwt-secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalStateException("APP_JWT_SECRET debe tener al menos 32 bytes.");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(bytes, "HmacSHA256")).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("agnostic-ticket-router"));
        return decoder;
    }
    @Bean SecurityFilterChain chain(HttpSecurity http) throws Exception {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles"); roles.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter(); converter.setJwtGrantedAuthoritiesConverter(roles);
        return http.csrf(c -> c.disable()).cors(c -> {}).authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health", "/actuator/info").permitAll().anyRequest().hasAuthority("ROLE_ADMIN"))
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))).build();
    }
    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.security.cors-origin:http://localhost:4200}") String origin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(origin)); configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**", configuration); return source;
    }
}
