package com.agnosticrouter.ingestion;

import com.agnosticrouter.contracts.IncomingEmailEvent;
import com.agnosticrouter.contracts.RabbitNames;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/v1/intake")
public class EmailIntakeController {
    private final RabbitTemplate rabbitTemplate;
    public EmailIntakeController(RabbitTemplate rabbitTemplate){ this.rabbitTemplate = rabbitTemplate; }

    @PostMapping("/emails")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String,String> receive(@Valid @RequestBody EmailRequest request){
        String requestId = UUID.randomUUID().toString();
        // The same Gmail message must keep its identity if the publisher retries before recording it.
        if(request.metadata()!=null && "gmail".equals(request.metadata().get("source"))
            && request.metadata().get("gmailMessageId") instanceof String gmailId
            && !gmailId.isBlank()) {
            requestId = UUID.nameUUIDFromBytes(
                (request.profileCode()+"|gmail|"+gmailId).getBytes(StandardCharsets.UTF_8)).toString();
        }
        IncomingEmailEvent event = new IncomingEmailEvent(
                requestId, request.profileCode(), request.from(),
                Optional.ofNullable(request.to()).orElse(List.of()),
                Optional.ofNullable(request.cc()).orElse(List.of()),
                request.subject(), request.body(),
                Optional.ofNullable(request.metadata()).orElse(Map.of()), Instant.now());
        rabbitTemplate.convertAndSend(RabbitNames.EXCHANGE, RabbitNames.RK_EMAIL_RECEIVED, event);
        return Map.of("requestId", requestId, "status", "ACCEPTED");
    }

    public record EmailRequest(
            @NotBlank String profileCode,
            @NotBlank @Email String from,
            List<String> to,
            List<String> cc,
            @NotBlank String subject,
            @NotBlank String body,
            Map<String,Object> metadata
    ){}
}
