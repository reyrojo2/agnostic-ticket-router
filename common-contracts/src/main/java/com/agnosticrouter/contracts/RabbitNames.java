package com.agnosticrouter.contracts;

public final class RabbitNames {
    private RabbitNames() {}
    public static final String EXCHANGE = "ticket.events";
    public static final String RK_EMAIL_RECEIVED = "email.received";
    public static final String RK_TICKET_STANDARDIZED = "ticket.standardized";
    public static final String RK_SYSTEM_EVENT = "system.event";
    public static final String Q_DECISION_EMAILS = "decision.email.received.q";
    public static final String Q_CRM_STANDARD_TICKETS = "crm.standard.tickets.q";
    public static final String Q_NOTIFICATION_EMAILS = "notification.email.received.q";
    public static final String Q_NOTIFICATION_SYSTEM = "notification.system.events.q";
}
