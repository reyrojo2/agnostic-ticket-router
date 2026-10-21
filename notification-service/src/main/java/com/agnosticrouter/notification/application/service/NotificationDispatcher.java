package com.agnosticrouter.notification.application.service;

import com.agnosticrouter.notification.application.port.NotificationSender;

import com.agnosticrouter.notification.domain.model.NotificationChannel;
import com.agnosticrouter.notification.domain.model.NotificationTemplate;
import com.agnosticrouter.notification.application.port.NotificationChannelStore;
import com.agnosticrouter.notification.application.port.NotificationTemplateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class NotificationDispatcher {
    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationTemplateStore templates;
    private final NotificationChannelStore channels;
    private final TextTemplateRenderer renderer;
    private final List<NotificationSender> senders;

    public NotificationDispatcher(
            NotificationTemplateStore templates,
            NotificationChannelStore channels,
            TextTemplateRenderer renderer,
            List<NotificationSender> senders
    ) {
        this.templates = templates;
        this.channels = channels;
        this.renderer = renderer;
        this.senders = List.copyOf(senders);
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

        String type = channel.getType().toUpperCase(Locale.ROOT);
        NotificationSender sender = senders.stream()
                .filter(candidate -> candidate.type().equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de canal no soportado: " + channel.getType()));
        sender.send(channel, recipient, subject, body);
    }
}
