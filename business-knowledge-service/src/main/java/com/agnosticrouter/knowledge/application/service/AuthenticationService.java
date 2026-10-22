package com.agnosticrouter.knowledge.application.service;


import com.agnosticrouter.knowledge.application.port.AccessTokenIssuer;
import java.util.Map;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/** Authentication use case; HTTP is handled by AuthController. */
@Service
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final AccessTokenIssuer jwtService;
    public AuthenticationService(AuthenticationManager authenticationManager, AccessTokenIssuer jwtService) {
        this.authenticationManager=authenticationManager;
        this.jwtService=jwtService;
    }
    public Map<String, Object> login(String username, String password) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        AccessTokenIssuer.TokenResult token = jwtService.issue(authentication);
        return Map.of(
                "authenticated", true,
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities().stream().map(Object::toString).toList(),
                "token", token.token(),
                "expiresAt", token.expiresAt()
        );
    }
}
