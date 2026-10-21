package com.agnosticrouter.crm.application.port;

import com.agnosticrouter.contracts.StandardTicketEvent;
import java.util.Map;

/** Publishes the outcome without exposing RabbitMQ to the use case. */
public interface CrmOutcomePublisher {
    void publish(StandardTicketEvent ticket, String type, String message, Map<String, Object> data);
}
