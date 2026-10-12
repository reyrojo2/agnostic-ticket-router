package com.agnosticrouter.notification.api;

import com.agnosticrouter.notification.domain.NotificationChannel;
import com.agnosticrouter.notification.domain.NotificationTemplate;
import com.agnosticrouter.notification.repo.NotificationChannelRepository;
import com.agnosticrouter.notification.repo.NotificationTemplateRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/notifications")
public class NotificationAdminController {
    private final NotificationChannelRepository channels;
    private final NotificationTemplateRepository templates;

    public NotificationAdminController(
            NotificationChannelRepository channels,
            NotificationTemplateRepository templates
    ) {
        this.channels = channels;
        this.templates = templates;
    }

    @GetMapping("/channels")
    public List<NotificationChannel> channels() {
        return channels.findAll();
    }

    @PostMapping("/channels")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationChannel createChannel(@Valid @RequestBody ChannelRequest request) {
        NotificationChannel channel = new NotificationChannel();
        apply(channel, request);
        return channels.save(channel);
    }

    @PutMapping("/channels/{id}")
    public NotificationChannel updateChannel(@PathVariable Long id, @Valid @RequestBody ChannelRequest request) {
        NotificationChannel channel = channels.findById(id).orElseThrow();
        apply(channel, request);
        return channels.save(channel);
    }

    @DeleteMapping("/channels/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChannel(@PathVariable Long id) {
        channels.deleteById(id);
    }

    @GetMapping("/templates")
    public List<NotificationTemplate> templates() {
        return templates.findAll();
    }

    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationTemplate createTemplate(@Valid @RequestBody TemplateRequest request) {
        NotificationTemplate template = new NotificationTemplate();
        apply(template, request);
        return templates.save(template);
    }

    @PutMapping("/templates/{id}")
    public NotificationTemplate updateTemplate(@PathVariable Long id, @Valid @RequestBody TemplateRequest request) {
        NotificationTemplate template = templates.findById(id).orElseThrow();
        apply(template, request);
        return templates.save(template);
    }

    @DeleteMapping("/templates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@PathVariable Long id) {
        templates.deleteById(id);
    }

    private void apply(NotificationChannel channel, ChannelRequest request) {
        channel.setCode(request.code());
        channel.setName(request.name());
        channel.setType(request.type());
        channel.setHost(request.host());
        channel.setPort(request.port());
        channel.setUsernameRef(request.usernameRef());
        channel.setPasswordRef(request.passwordRef());
        channel.setFromAddress(request.fromAddress());
        channel.setSmtpAuth(request.smtpAuth());
        channel.setStartTls(request.startTls());
        channel.setActive(request.active());
    }

    private void apply(NotificationTemplate template, TemplateRequest request) {
        template.setProfileCode(request.profileCode());
        template.setEventType(request.eventType());
        template.setChannelCode(request.channelCode());
        template.setRecipientTemplate(request.recipientTemplate());
        template.setSubjectTemplate(request.subjectTemplate());
        template.setBodyTemplate(request.bodyTemplate());
        template.setActive(request.active());
    }

    public record ChannelRequest(
            @NotBlank String code,
            @NotBlank String name,
            String type,
            String host,
            Integer port,
            String usernameRef,
            String passwordRef,
            String fromAddress,
            Boolean smtpAuth,
            Boolean startTls,
            Boolean active
    ) {
        public ChannelRequest {
            if (type == null) type = "LOG";
            if (port == null) port = 587;
            if (smtpAuth == null) smtpAuth = true;
            if (startTls == null) startTls = true;
            if (active == null) active = true;
        }
    }

    public record TemplateRequest(
            String profileCode,
            @NotBlank String eventType,
            @NotBlank String channelCode,
            @NotBlank String recipientTemplate,
            @NotBlank String subjectTemplate,
            @NotBlank String bodyTemplate,
            Boolean active
    ) {
        public TemplateRequest {
            if (profileCode == null || profileCode.isBlank()) profileCode = "*";
            if (active == null) active = true;
        }
    }
}
