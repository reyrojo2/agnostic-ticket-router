package com.agnosticrouter.notification.gateway;

import com.agnosticrouter.contracts.IncomingEmailEvent;
import com.agnosticrouter.contracts.RabbitNames;
import com.agnosticrouter.contracts.SystemEvent;
import com.agnosticrouter.notification.service.NotificationDispatcher;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class NotificationListener {
    private final NotificationDispatcher dispatcher;

    public NotificationListener(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @RabbitListener(queues = RabbitNames.Q_NOTIFICATION_EMAILS)
    public void received(IncomingEmailEvent event) {
        dispatcher.dispatch(event.profileCode(), "EMAIL_RECEIVED", event);
    }

    @RabbitListener(queues = RabbitNames.Q_NOTIFICATION_SYSTEM)
    public void system(SystemEvent event) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("requestId", event.requestId());
        context.put("profileCode", event.profileCode());
        context.put("recipient", event.recipient());
        context.put("type", event.type());
        context.put("message", event.message());
        context.put("data", event.data());
        context.put("occurredAt", event.occurredAt());
        dispatcher.dispatch(event.profileCode(), event.type(), context);
    }
}
