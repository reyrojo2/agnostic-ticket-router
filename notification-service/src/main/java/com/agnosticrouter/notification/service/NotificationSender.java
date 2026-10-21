package com.agnosticrouter.notification.service;

import com.agnosticrouter.notification.domain.NotificationChannel;

/** Output port for channel-specific delivery. */
public interface NotificationSender {
    String type();
    void send(NotificationChannel channel, String recipient, String subject, String body);
}
