package com.agnosticrouter.notification.infrastructure.persistence;

import com.agnosticrouter.notification.application.port.NotificationTemplateStore;

import com.agnosticrouter.notification.domain.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long>, NotificationTemplateStore {
    Optional<NotificationTemplate> findFirstByProfileCodeAndEventTypeAndActiveTrue(String profileCode, String eventType);
}
