package com.agnosticrouter.gmail.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    JwtDecoder jwtDecoder(@Value("${app.jwt-secret}") String secret) {
        byte[] bytes=secret.getBytes(StandardCharsets.UTF_8);
        if(bytes.length<32)throw new IllegalArgumentException("APP_JWT_SECRET debe tener al menos 32 bytes");
        NimbusJwtDecoder decoder=NimbusJwtDecoder.withSecretKey(new SecretKeySpec(bytes,"HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("agnostic-ticket-router"));
        return decoder;
    }
    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        JwtGrantedAuthoritiesConverter roles=new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");roles.setAuthorityPrefix("");
        JwtAuthenticationConverter converter=new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return http.csrf(c->c.disable())
            .authorizeHttpRequests(a->a.requestMatchers("/api/v1/gmail/oauth/callback",
                    "/actuator/health","/actuator/info").permitAll()
                    .anyRequest().hasAuthority("ROLE_ADMIN"))
            .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter))).build();
    }
}
