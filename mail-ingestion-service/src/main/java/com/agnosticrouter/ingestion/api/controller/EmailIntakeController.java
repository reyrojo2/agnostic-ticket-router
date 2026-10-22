package com.agnosticrouter.ingestion.api.controller;

import com.agnosticrouter.ingestion.domain.model.EmailIntakeCommand;
import com.agnosticrouter.ingestion.application.service.EmailIntakeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/intake")
public class EmailIntakeController {
    private final EmailIntakeService service;

    public EmailIntakeController(EmailIntakeService service) {
        this.service = service;
    }

    @PostMapping("/emails")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> receive(@Valid @RequestBody EmailRequest request) {
        return Map.of("requestId", service.receive(request.toCommand()), "status", "ACCEPTED");
    }

    public record EmailRequest(
            @NotBlank String profileCode,
            @NotBlank @Email String from,
            List<String> to,
            List<String> cc,
            @NotBlank String subject,
            @NotBlank String body,
            Map<String, Object> metadata
    ) {
        public EmailIntakeCommand toCommand() {
            return new EmailIntakeCommand(profileCode, from, to, cc, subject, body, metadata);
        }
    }
}
