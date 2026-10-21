package com.agnosticrouter.decision.gateway;

import com.agnosticrouter.contracts.IncomingEmailEvent;
import com.agnosticrouter.contracts.RabbitNames;
import com.agnosticrouter.decision.service.ProcessIncomingEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Transport adapter; delegates all classification and deduplication to the use case. */
@Component
public class DecisionListener {
    private static final Logger log=LoggerFactory.getLogger(DecisionListener.class);
    private final ProcessIncomingEmail processor;
    private final RabbitTemplate rabbit;
    public DecisionListener(ProcessIncomingEmail processor, RabbitTemplate rabbit) {
        this.processor=processor;this.rabbit=rabbit;
    }
    @RabbitListener(queues=RabbitNames.Q_DECISION_EMAILS, concurrency="1")
    public void handle(IncomingEmailEvent email) {
        try {
            if(!processor.process(email)) log.info("Skipping previously classified requestId={}",email.requestId());
        } catch(RuntimeException ex) {
            log.error("Decision failure for requestId={}; moving to quarantine",email.requestId(),ex);
            rabbit.convertAndSend(RabbitNames.EXCHANGE,"decision.email.failed",email);
        }
    }
}
