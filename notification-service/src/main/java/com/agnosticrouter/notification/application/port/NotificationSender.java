package com.agnosticrouter.notification.application.port;

import com.agnosticrouter.notification.domain.model.NotificationChannel;

/** Output port for channel-specific delivery. */
public interface NotificationSender {
    String type();
    void send(NotificationChannel channel, String recipient, String subject, String body);
}
