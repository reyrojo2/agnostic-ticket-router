package com.agnosticrouter.notification.application.port;

import com.agnosticrouter.notification.domain.model.NotificationTemplate;

/** Persistence contract owned by this microservice's application layer. */
public interface NotificationTemplateStore {
    java.util.List<NotificationTemplate> findAll();
    java.util.Optional<NotificationTemplate> findById(Long id);
    java.util.Optional<NotificationTemplate> findFirstByProfileCodeAndEventTypeAndActiveTrue(String profileCode, String eventType);
    <S extends NotificationTemplate> S save(S entity);
    void deleteById(Long id);
}
