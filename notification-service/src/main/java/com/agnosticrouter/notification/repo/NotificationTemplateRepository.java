package com.agnosticrouter.notification.repo;

import com.agnosticrouter.notification.domain.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findFirstByProfileCodeAndEventTypeAndActiveTrue(String profileCode, String eventType);
}
