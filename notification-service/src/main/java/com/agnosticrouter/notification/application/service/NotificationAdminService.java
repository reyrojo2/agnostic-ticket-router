package com.agnosticrouter.notification.application.service;

import com.agnosticrouter.notification.domain.model.NotificationChannel;
import com.agnosticrouter.notification.domain.model.NotificationTemplate;
import com.agnosticrouter.notification.application.port.NotificationChannelStore;
import com.agnosticrouter.notification.application.port.NotificationTemplateStore;
import com.agnosticrouter.notification.application.port.NotificationSecretProtector;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;


import java.util.List;
@Service
public class NotificationAdminService {
    private final NotificationChannelStore channels;
    private final NotificationTemplateStore templates;
    private final NotificationSecretProtector secrets;
    private final NotificationMessageCatalog catalog;

    public NotificationAdminService(
            NotificationChannelStore channels,
            NotificationTemplateStore templates, NotificationSecretProtector secrets, NotificationMessageCatalog catalog
    ) {
        this.channels = channels;
        this.templates = templates;
        this.secrets=secrets;this.catalog=catalog;
    }
    public List<NotificationChannel> channels() {
        return channels.findAll();
    }
    public NotificationChannel createChannel(ChannelRequest request) {
        NotificationChannel channel = new NotificationChannel();
        apply(channel, request);
        return channels.save(channel);
    }
    public NotificationChannel updateChannel(Long id, ChannelRequest request) {
        NotificationChannel channel = channels.findById(id).orElseThrow();
        apply(channel, request);
        return channels.save(channel);
    }
    public void deleteChannel(Long id) {
        var channel=channels.findById(id).orElseThrow();
        if(templates.findAll().stream().anyMatch(t->t.getChannelCode().equals(channel.getCode())))throw new IllegalArgumentException("Este canal tiene mensajes asociados. Cambia su canal o elimina esos mensajes primero.");
        channels.deleteById(id);
    }
    public List<NotificationTemplate> templates() {
        return templates.findAll();
    }
    public NotificationTemplate createTemplate(TemplateRequest request) {
        NotificationTemplate template = new NotificationTemplate();
        apply(template, request);
        return templates.save(template);
    }
    public NotificationTemplate updateTemplate(Long id, TemplateRequest request) {
        NotificationTemplate template = templates.findById(id).orElseThrow();
        apply(template, request);
        return templates.save(template);
    }
    public void deleteTemplate(Long id) {
        templates.findById(id).orElseThrow();
        templates.deleteById(id);
    }

    private void apply(NotificationChannel channel, ChannelRequest request) {
        if(!request.code().matches("[A-Za-z0-9_-]{1,80}")||request.name().length()>160)throw new IllegalArgumentException("El nombre o identificador del canal no es válido.");
        if(channel.getId()!=null&&!channel.getCode().equals(request.code()))throw new IllegalArgumentException("El identificador del canal se conserva para mantener sus mensajes asociados.");
        if(!java.util.Set.of("LOG","SMTP").contains(request.type()))throw new IllegalArgumentException("Selecciona correo electrónico o registro de desarrollo.");
        if(channel.getId()!=null&&!request.active()&&templates.findAll().stream().anyMatch(t->t.getChannelCode().equals(channel.getCode())&&Boolean.TRUE.equals(t.getActive())))throw new IllegalArgumentException("Desactiva o cambia de canal los mensajes activos antes de deshabilitar este canal.");
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
        channel.setSslEnabled(request.sslEnabled());
        if(request.removeCredentials()){
            channel.setEncryptedUsername(null);channel.setEncryptedPassword(null);channel.setUsernameRef(null);channel.setPasswordRef(null);
        }
        if(request.username()!=null&&!request.username().isBlank()){
            if(request.username().length()>500||request.username().contains("\n")||request.username().contains("\r"))throw new IllegalArgumentException("El usuario SMTP no es válido.");
            channel.setEncryptedUsername(secrets.encrypt(request.username().trim()));channel.setUsernameRef(null);
        }
        if(request.password()!=null&&!request.password().isEmpty()){
            if(request.password().length()>4096)throw new IllegalArgumentException("La contraseña SMTP supera la longitud permitida.");
            channel.setEncryptedPassword(secrets.encrypt(request.password()));channel.setPasswordRef(null);
        }
        for(String ref:new String[]{channel.getUsernameRef(),channel.getPasswordRef()})if(ref!=null&&!ref.isBlank()&&!ref.matches("ENV:[A-Za-z_][A-Za-z0-9_]*"))throw new IllegalArgumentException("La referencia avanzada debe usar ENV:NOMBRE_VARIABLE.");
        if("SMTP".equals(request.type())){
            if(request.host()==null||!request.host().matches("[A-Za-z0-9.:_\\[\\]-]{1,500}"))throw new IllegalArgumentException("Indica el servidor SMTP sin http:// ni rutas.");
            if(request.port()<1||request.port()>65535)throw new IllegalArgumentException("El puerto SMTP debe estar entre 1 y 65535.");
            if(request.fromAddress()==null||!request.fromAddress().matches("[^\\s@<>]+@[^\\s@<>]+"))throw new IllegalArgumentException("Indica un correo de remitente válido.");
            if(request.startTls()&&request.sslEnabled())throw new IllegalArgumentException("Elige STARTTLS o TLS directo, no ambos.");
            if(request.smtpAuth()&&(!channel.isUsernameConfigured()||!channel.isPasswordConfigured()))throw new IllegalArgumentException("Introduce el usuario y la contraseña SMTP o selecciona Sin autenticación.");
        }
        channel.setActive(request.active());
    }

    private void apply(NotificationTemplate template, TemplateRequest request) {
        catalog.validate(request.eventType(),request.recipientTemplate(),request.subjectTemplate(),request.bodyTemplate());
        var channel=channels.findAll().stream().filter(c->c.getCode().equals(request.channelCode())).findFirst().orElseThrow(()->new IllegalArgumentException("Selecciona un canal guardado."));
        if(request.active()&&!Boolean.TRUE.equals(channel.getActive()))throw new IllegalArgumentException("Selecciona un canal habilitado para activar el mensaje.");
        if(templates.findAll().stream().anyMatch(t->!java.util.Objects.equals(t.getId(),template.getId())&&t.getProfileCode().equals(request.profileCode())&&t.getEventType().equals(request.eventType())))throw new IllegalArgumentException("Ya existe un mensaje para este perfil y momento. Edítalo desde la lista.");
        if(request.profileCode().length()>80||request.recipientTemplate().length()>1000||request.subjectTemplate().length()>1000||request.subjectTemplate().contains("\n")||request.subjectTemplate().contains("\r")||request.bodyTemplate().length()>100000)throw new IllegalArgumentException("Revisa la longitud del mensaje y que el asunto ocupe una sola línea.");
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
            Boolean active,
            String username,
            String password,
            Boolean removeCredentials,
            Boolean sslEnabled
    ) {
        public ChannelRequest {
            if (type == null) type = "LOG";
            if (port == null) port = 587;
            if (smtpAuth == null) smtpAuth = true;
            if (startTls == null) startTls = true;
            if (active == null) active = true;
            if (removeCredentials == null) removeCredentials = false;
            if (sslEnabled == null) sslEnabled = false;
        }
        @Override public String toString(){return "ChannelRequest[credentials=REDACTED]";}
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
