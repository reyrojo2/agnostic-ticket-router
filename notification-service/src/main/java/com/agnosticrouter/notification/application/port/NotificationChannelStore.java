package com.agnosticrouter.notification.application.port;

import com.agnosticrouter.notification.domain.model.NotificationChannel;

/** Persistence contract owned by this microservice's application layer. */
public interface NotificationChannelStore {
    java.util.List<NotificationChannel> findAll();
    java.util.Optional<NotificationChannel> findById(Long id);
    java.util.Optional<NotificationChannel> findByCodeAndActiveTrue(String code);
    <S extends NotificationChannel> S save(S entity);
    void deleteById(Long id);
}
