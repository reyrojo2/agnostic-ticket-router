package com.agnosticrouter.decision.service;

import com.agnosticrouter.contracts.IncomingEmailEvent;
import com.agnosticrouter.contracts.RabbitNames;
import com.agnosticrouter.contracts.StandardTicketEvent;
import com.agnosticrouter.decision.GenericDecisionEngine;
import com.agnosticrouter.decision.repo.ProcessedDecisionRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/** Application use case, independent of the AMQP listener lifecycle. */
@Service
public class ProcessIncomingEmail {
    private final ProcessedDecisionRepository processed;
    private final GenericDecisionEngine engine;
    private final RabbitTemplate publisher;

    public ProcessIncomingEmail(ProcessedDecisionRepository processed, GenericDecisionEngine engine,
                                RabbitTemplate publisher) {
        this.processed=processed; this.engine=engine; this.publisher=publisher;
    }
    public boolean process(IncomingEmailEvent email) {
        if(email.requestId()==null || email.requestId().isBlank()) throw new IllegalArgumentException("Missing requestId");
        if(processed.isProcessed(email.requestId())) return false;
        StandardTicketEvent ticket=engine.evaluate(email);
        publisher.convertAndSend(RabbitNames.EXCHANGE,RabbitNames.RK_TICKET_STANDARDIZED,ticket);
        processed.markProcessed(email.requestId());
        return true;
    }
}
