package com.agnosticrouter.notification.gateway;

import com.agnosticrouter.notification.domain.NotificationChannel;
import com.agnosticrouter.notification.service.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogNotificationSender implements NotificationSender {
    private static final Logger log = LoggerFactory.getLogger(LogNotificationSender.class);
    @Override public String type() { return "LOG"; }
    @Override public void send(NotificationChannel channel, String recipient, String subject, String body) {
        log.info("[NOTIFICACION LOG] to={} subject={} body={}", recipient, subject, body);
    }
}
