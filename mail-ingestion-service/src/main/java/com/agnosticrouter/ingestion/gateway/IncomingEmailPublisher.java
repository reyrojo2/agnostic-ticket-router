package com.agnosticrouter.ingestion.gateway;

import com.agnosticrouter.contracts.IncomingEmailEvent;

/** Output port: the application does not depend on RabbitMQ. */
public interface IncomingEmailPublisher {
    void publish(IncomingEmailEvent event);
}
