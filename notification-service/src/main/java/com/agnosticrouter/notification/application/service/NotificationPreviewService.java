package com.agnosticrouter.notification.application.service;

import com.agnosticrouter.contracts.*;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service
public class NotificationPreviewService {
    private final NotificationContextFactory contexts;
    private final NotificationMessageCatalog catalog;
    private final TextTemplateRenderer renderer;
    public NotificationPreviewService(NotificationContextFactory contexts,NotificationMessageCatalog catalog,TextTemplateRenderer renderer){this.contexts=contexts;this.catalog=catalog;this.renderer=renderer;}
    public Preview preview(NotificationAdminService.TemplateRequest request){
        catalog.validate(request.eventType(),request.recipientTemplate(),request.subjectTemplate(),request.bodyTemplate());
        Object context="EMAIL_RECEIVED".equals(request.eventType())
                ?contexts.received(new IncomingEmailEvent("referencia-interna-de-ejemplo",request.profileCode(),"cliente@ejemplo.test",List.of(),List.of(),"Consulta sobre mi servicio","Mensaje de ejemplo",Map.of(),Instant.now()))
                :contexts.system(new SystemEvent("referencia-interna-de-ejemplo",request.profileCode(),"cliente@ejemplo.test",request.eventType(),"Mensaje de ejemplo",
                    "CRM_CREATED".equals(request.eventType())?Map.of("externalTicketId","id-tecnico-del-ejemplo","externalTicketNumber","12345","ticketReference","12345"):Map.of(),Instant.now()));
        return new Preview(renderer.render(request.recipientTemplate(),context),renderer.render(request.subjectTemplate(),context),renderer.render(request.bodyTemplate(),context));
    }
    public record Preview(String recipient,String subject,String body){}
}
