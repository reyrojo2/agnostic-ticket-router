package com.agnosticrouter.ingestion.service;

import com.agnosticrouter.contracts.IncomingEmailEvent;
import com.agnosticrouter.ingestion.domain.EmailIntakeCommand;
import com.agnosticrouter.ingestion.gateway.IncomingEmailPublisher;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EmailIntakeService {
    private final IncomingEmailPublisher publisher;

    public EmailIntakeService(IncomingEmailPublisher publisher) {
        this.publisher = publisher;
    }

    public String receive(EmailIntakeCommand command) {
        String requestId = requestIdFor(command);
        IncomingEmailEvent event = new IncomingEmailEvent(
                requestId, command.profileCode(), command.from(),
                Optional.ofNullable(command.to()).orElse(List.of()),
                Optional.ofNullable(command.cc()).orElse(List.of()),
                command.subject(), command.body(),
                Optional.ofNullable(command.metadata()).orElse(Map.of()), Instant.now());
        publisher.publish(event);
        return requestId;
    }

    /** Gmail IDs are stable across retries; API-submitted requests get new IDs. */
    String requestIdFor(EmailIntakeCommand command) {
        if (command.metadata() != null
                && "gmail".equals(command.metadata().get("source"))
                && command.metadata().get("gmailMessageId") instanceof String gmailId
                && !gmailId.isBlank()) {
            return UUID.nameUUIDFromBytes(
                    (command.profileCode() + "|gmail|" + gmailId)
                            .getBytes(StandardCharsets.UTF_8)).toString();
        }
        return UUID.randomUUID().toString();
    }
}
