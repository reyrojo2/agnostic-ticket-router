package com.agnosticrouter.crm.infrastructure.messaging;

import com.agnosticrouter.contracts.*;
import com.agnosticrouter.crm.application.port.CrmOutcomePublisher;
import java.time.Instant;
import java.util.Map;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitCrmOutcomePublisher implements CrmOutcomePublisher {
    private final RabbitTemplate rabbit;
    public RabbitCrmOutcomePublisher(RabbitTemplate rabbit) { this.rabbit=rabbit; }

    @Override
    public void publish(StandardTicketEvent ticket, String type, String message, Map<String, Object> data) {
        rabbit.convertAndSend(
                RabbitNames.EXCHANGE,
                RabbitNames.RK_SYSTEM_EVENT,
                new SystemEvent(
                        ticket.requestId(),
                        ticket.profileCode(),
                        ticket.sender(),
                        type,
                        message,
                        data,
                        Instant.now()
                )
        );
    }
}
