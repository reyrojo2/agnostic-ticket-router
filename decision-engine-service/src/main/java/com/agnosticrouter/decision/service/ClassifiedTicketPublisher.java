package com.agnosticrouter.decision.service;

import com.agnosticrouter.contracts.StandardTicketEvent;

/** Output port for a classified ticket; implementations choose the transport. */
public interface ClassifiedTicketPublisher {
    void publish(StandardTicketEvent ticket);
}
