package com.agnosticrouter.notification.application.service;

import com.agnosticrouter.contracts.SystemEvent;
import com.agnosticrouter.notification.application.port.*;
import com.agnosticrouter.notification.domain.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class NotificationDispatcherTest {
    @Test void explicitlyDisabledProfileMessageDoesNotFallBackToAGeneralMessage() {
        var templates=mock(NotificationTemplateStore.class);var channels=mock(NotificationChannelStore.class);var sender=mock(NotificationSender.class);
        when(templates.existsByProfileCodeAndEventTypeAndActiveFalse("P","CRM_CREATED")).thenReturn(true);
        new NotificationDispatcher(templates,channels,new TextTemplateRenderer(new ObjectMapper()),List.of(sender)).dispatch("P","CRM_CREATED",Map.of());
        verifyNoInteractions(channels,sender);verify(templates,never()).findFirstByProfileCodeAndEventTypeAndActiveTrue(anyString(),anyString());
    }
    @Test void dispatchesTheConfiguredProfileMessageUsingItsSavedChannelAndCrmReference() {
        var templates=mock(NotificationTemplateStore.class);var channels=mock(NotificationChannelStore.class);var sender=mock(NotificationSender.class);
        var template=new NotificationTemplate();template.setProfileCode("P");template.setEventType("CRM_CREATED");template.setChannelCode("MAIL");template.setRecipientTemplate("{{recipient}}");template.setSubjectTemplate("{{confirmationSubject}}");template.setBodyTemplate("{{confirmationMessage}}");
        var channel=new NotificationChannel();channel.setCode("MAIL");channel.setType("SMTP");
        when(templates.findFirstByProfileCodeAndEventTypeAndActiveTrue("P","CRM_CREATED")).thenReturn(Optional.of(template));when(channels.findByCodeAndActiveTrue("MAIL")).thenReturn(Optional.of(channel));when(sender.type()).thenReturn("SMTP");
        var context=new NotificationContextFactory().system(new SystemEvent("request-uuid","P","client@example.test","CRM_CREATED","Created",Map.of("externalTicketId","technical-id","externalTicketNumber","5"),Instant.now()));
        new NotificationDispatcher(templates,channels,new TextTemplateRenderer(new ObjectMapper().findAndRegisterModules()),List.of(sender)).dispatch("P","CRM_CREATED",context);
        verify(sender).send(eq(channel),eq("client@example.test"),eq("Caso #5 registrado"),argThat(body->body.contains("número de caso es 5")&&!body.contains("request-uuid")&&!body.contains("technical-id")));
    }
}
