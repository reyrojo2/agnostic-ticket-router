package com.agnosticrouter.notification.domain.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_channel")
@Getter
@Setter
@NoArgsConstructor
public class NotificationChannel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 30)
    private String type = "LOG";

    @Column(length = 500)
    private String host;

    private Integer port = 587;

    @Column(name = "username_ref", length = 500)
    private String usernameRef;

    @Column(name = "password_ref", length = 500)
    private String passwordRef;

    @Column(name = "from_address", length = 320)
    private String fromAddress;

    @Column(name = "smtp_auth", nullable = false)
    private Boolean smtpAuth = true;

    @Column(name = "start_tls", nullable = false)
    private Boolean startTls = true;

    @Column(name = "ssl_enabled", nullable = false)
    private Boolean sslEnabled = false;

    @JsonIgnore
    @Column(name = "encrypted_username", columnDefinition = "TEXT")
    private String encryptedUsername;

    @JsonIgnore
    @Column(name = "encrypted_password", columnDefinition = "TEXT")
    private String encryptedPassword;

    public boolean isUsernameConfigured() { return encryptedUsername != null || usernameRef != null && !usernameRef.isBlank(); }
    public boolean isPasswordConfigured() { return encryptedPassword != null || passwordRef != null && !passwordRef.isBlank(); }

    @Column(nullable = false)
    private Boolean active = true;
}
