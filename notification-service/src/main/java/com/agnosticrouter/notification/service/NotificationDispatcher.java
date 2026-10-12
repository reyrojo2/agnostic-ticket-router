package com.agnosticrouter.notification.service;

import com.agnosticrouter.notification.domain.NotificationChannel;
import com.agnosticrouter.notification.domain.NotificationTemplate;
import com.agnosticrouter.notification.repo.NotificationChannelRepository;
import com.agnosticrouter.notification.repo.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class NotificationDispatcher {
    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationTemplateRepository templates;
    private final NotificationChannelRepository channels;
    private final TextTemplateRenderer renderer;
    private final SecretResolver secrets;

    public NotificationDispatcher(
            NotificationTemplateRepository templates,
            NotificationChannelRepository channels,
            TextTemplateRenderer renderer,
            SecretResolver secrets
    ) {
        this.templates = templates;
        this.channels = channels;
        this.renderer = renderer;
        this.secrets = secrets;
    }

    public void dispatch(String profileCode, String eventType, Object context) {
        NotificationTemplate template = templates
                .findFirstByProfileCodeAndEventTypeAndActiveTrue(profileCode, eventType)
                .or(() -> templates.findFirstByProfileCodeAndEventTypeAndActiveTrue("*", eventType))
                .orElse(null);

        if (template == null) {
            log.info("No existe plantilla de notificación para profile={} event={}", profileCode, eventType);
            return;
        }

        NotificationChannel channel = channels.findByCodeAndActiveTrue(template.getChannelCode())
                .orElseThrow(() -> new IllegalStateException(
                        "Canal de notificación inactivo o inexistente: " + template.getChannelCode()
                ));

        String recipient = renderer.render(template.getRecipientTemplate(), context);
        String subject = renderer.render(template.getSubjectTemplate(), context);
        String body = renderer.render(template.getBodyTemplate(), context);

        if (recipient == null || recipient.isBlank()) {
            log.warn("Notificación omitida porque el destinatario quedó vacío. profile={} event={}", profileCode, eventType);
            return;
        }

        String type = channel.getType().toUpperCase();
        if ("LOG".equals(type)) {
            log.info("[NOTIFICACION LOG] to={} subject={} body={}", recipient, subject, body);
            return;
        }
        if (!"SMTP".equals(type)) {
            throw new IllegalArgumentException("Tipo de canal no soportado: " + channel.getType());
        }

        JavaMailSenderImpl sender = createMailSender(channel);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(channel.getFromAddress());
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
    }

    private JavaMailSenderImpl createMailSender(NotificationChannel channel) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(channel.getHost());
        sender.setPort(channel.getPort());
        sender.setUsername(secrets.resolve(channel.getUsernameRef()));
        sender.setPassword(secrets.resolve(channel.getPasswordRef()));

        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", Boolean.toString(Boolean.TRUE.equals(channel.getSmtpAuth())));
        properties.put("mail.smtp.starttls.enable", Boolean.toString(Boolean.TRUE.equals(channel.getStartTls())));
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        return sender;
    }
}
