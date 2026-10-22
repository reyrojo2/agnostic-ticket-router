package com.agnosticrouter.notification.infrastructure.messaging;

import com.agnosticrouter.contracts.IncomingEmailEvent;
import com.agnosticrouter.contracts.RabbitNames;
import com.agnosticrouter.contracts.SystemEvent;
import com.agnosticrouter.notification.application.service.NotificationDispatcher;
import com.agnosticrouter.notification.application.service.NotificationContextFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class NotificationListener {
    private final NotificationDispatcher dispatcher;
    private final NotificationContextFactory contexts;

    public NotificationListener(NotificationDispatcher dispatcher, NotificationContextFactory contexts) {
        this.dispatcher = dispatcher;this.contexts=contexts;
    }

    @RabbitListener(queues = RabbitNames.Q_NOTIFICATION_EMAILS)
    public void received(IncomingEmailEvent event) {
        dispatcher.dispatch(event.profileCode(), "EMAIL_RECEIVED", contexts.received(event));
    }

    @RabbitListener(queues = RabbitNames.Q_NOTIFICATION_SYSTEM)
    public void system(SystemEvent event) {
        dispatcher.dispatch(event.profileCode(), event.type(), contexts.system(event));
    }
}
