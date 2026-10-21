package com.agnosticrouter.notification.infrastructure.persistence;

import com.agnosticrouter.notification.application.port.NotificationChannelStore;

import com.agnosticrouter.notification.domain.model.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, Long>, NotificationChannelStore {
    Optional<NotificationChannel> findByCodeAndActiveTrue(String code);
}
