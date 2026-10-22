package com.agnosticrouter.decision.application.service;

import com.agnosticrouter.decision.application.port.ClassifiedTicketPublisher;

import com.agnosticrouter.contracts.IncomingEmailEvent;
import com.agnosticrouter.contracts.StandardTicketEvent;

import com.agnosticrouter.decision.application.port.ProcessedDecisionStore;
import org.springframework.stereotype.Service;

/** Application use case, independent of the AMQP listener lifecycle. */
@Service
public class ProcessIncomingEmail {
    private final ProcessedDecisionStore processed;
    private final GenericDecisionEngine engine;
    private final ClassifiedTicketPublisher publisher;

    public ProcessIncomingEmail(ProcessedDecisionStore processed, GenericDecisionEngine engine,
                                ClassifiedTicketPublisher publisher) {
        this.processed=processed; this.engine=engine; this.publisher=publisher;
    }
    public boolean process(IncomingEmailEvent email) {
        if(email.requestId()==null || email.requestId().isBlank()) throw new IllegalArgumentException("Missing requestId");
        if(processed.isProcessed(email.requestId())) return false;
        StandardTicketEvent ticket=engine.evaluate(email);
        publisher.publish(ticket);
        processed.markProcessed(email.requestId());
        return true;
    }
}
