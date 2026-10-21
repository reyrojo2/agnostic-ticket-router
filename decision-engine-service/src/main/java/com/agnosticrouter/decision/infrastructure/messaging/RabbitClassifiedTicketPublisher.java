package com.agnosticrouter.decision.infrastructure.messaging;

import com.agnosticrouter.contracts.RabbitNames;
import com.agnosticrouter.contracts.StandardTicketEvent;
import com.agnosticrouter.decision.application.port.ClassifiedTicketPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitClassifiedTicketPublisher implements ClassifiedTicketPublisher {
    private final RabbitTemplate rabbit;
    public RabbitClassifiedTicketPublisher(RabbitTemplate rabbit) { this.rabbit = rabbit; }

    @Override
    public void publish(StandardTicketEvent ticket) {
        rabbit.convertAndSend(RabbitNames.EXCHANGE, RabbitNames.RK_TICKET_STANDARDIZED, ticket);
    }
}
