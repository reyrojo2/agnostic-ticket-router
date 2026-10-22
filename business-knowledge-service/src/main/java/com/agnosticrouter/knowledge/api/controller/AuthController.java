package com.agnosticrouter.knowledge.api.controller;

import com.agnosticrouter.knowledge.application.service.AuthenticationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationService authentication;
    public AuthController(AuthenticationService authentication) { this.authentication=authentication; }
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        return authentication.login(request.username(), request.password());
    }
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
}
