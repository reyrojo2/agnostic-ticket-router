package com.agnosticrouter.ingestion.infrastructure.messaging;


import com.agnosticrouter.ingestion.application.port.IncomingEmailPublisher;
import com.agnosticrouter.contracts.IncomingEmailEvent;
import com.agnosticrouter.contracts.RabbitNames;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitIncomingEmailPublisher implements IncomingEmailPublisher {
    private final RabbitTemplate rabbit;

    public RabbitIncomingEmailPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void publish(IncomingEmailEvent event) {
        rabbit.convertAndSend(RabbitNames.EXCHANGE, RabbitNames.RK_EMAIL_RECEIVED, event);
    }
}
