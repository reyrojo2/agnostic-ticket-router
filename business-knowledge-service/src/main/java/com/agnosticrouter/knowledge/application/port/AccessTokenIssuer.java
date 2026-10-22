package com.agnosticrouter.knowledge.application.port;

import java.time.Instant;
import org.springframework.security.core.Authentication;

/** Authentication token issued after a successful credential check. */
public interface AccessTokenIssuer {
    TokenResult issue(Authentication authentication);
    record TokenResult(String token, Instant expiresAt) {}
}
