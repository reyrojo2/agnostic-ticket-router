package com.agnosticrouter.knowledge;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public JwtService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public TokenResult issue(Authentication authentication) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.expirationMinutes(), ChronoUnit.MINUTES);
        String roles = authentication.getAuthorities().stream()
                .map(Object::toString)
                .reduce((left, right) -> left + " " + right)
                .orElse("");

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("agnostic-ticket-router")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(authentication.getName())
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResult(token, expiresAt);
    }

    public record TokenResult(String token, Instant expiresAt) {}
}
