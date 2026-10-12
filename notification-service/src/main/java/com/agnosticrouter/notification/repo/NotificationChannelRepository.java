package com.agnosticrouter.notification.repo;

import com.agnosticrouter.notification.domain.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, Long> {
    Optional<NotificationChannel> findByCodeAndActiveTrue(String code);
}
