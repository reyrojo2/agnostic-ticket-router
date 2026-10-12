package com.agnosticrouter.notification.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_template")
@Getter
@Setter
@NoArgsConstructor
public class NotificationTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_code", nullable = false, length = 80)
    private String profileCode = "*";

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "channel_code", nullable = false, length = 80)
    private String channelCode;

    @Column(name = "recipient_template", nullable = false, length = 1000)
    private String recipientTemplate;

    @Column(name = "subject_template", nullable = false, columnDefinition = "TEXT")
    private String subjectTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    @Column(nullable = false)
    private Boolean active = true;

}
