package com.agnosticrouter.notification.application.service;

import com.agnosticrouter.contracts.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NotificationMessagesTest {
    private final NotificationContextFactory contexts=new NotificationContextFactory();
    private final NotificationMessageCatalog catalog=new NotificationMessageCatalog();
    private final TextTemplateRenderer renderer=new TextTemplateRenderer(new ObjectMapper().findAndRegisterModules());
    @Test void confirmationUsesTheRealCrmNumberInSubjectAndBodyInsteadOfEitherTechnicalId() {
        var event=new SystemEvent("long-request-uuid","TELCO","customer@example.test","CRM_CREATED","Created",Map.of("externalTicketId","technical-crm-id","externalTicketNumber","5","ticketReference","5"),Instant.now());
        var context=contexts.system(event);var definition=catalog.require("CRM_CREATED");
        assertEquals("Caso #5 registrado",renderer.render(definition.subject(),context));
        String body=renderer.render(definition.body(),context);
        assertTrue(body.contains("número de caso es 5"));assertFalse(body.contains("long-request-uuid"));assertFalse(body.contains("technical-crm-id"));
        assertEquals("technical-crm-id",((Map<?,?>)context.get("data")).get("externalTicketId"));
    }
    @Test void missingExpectedNumberNeverTurnsTheRequestUuidOrTechnicalIdIntoACaseNumber() {
        var context=contexts.system(new SystemEvent("long-request-uuid","P","customer@example.test","CRM_CREATED","Created",Map.of("externalTicketId","technical-crm-id","ticketReference",""),Instant.now()));
        assertEquals("",context.get("ticketReference"));assertEquals("Solicitud registrada en el CRM",context.get("confirmationSubject"));
        assertFalse(context.get("confirmationMessage").toString().contains("technical-crm-id"));
    }
    @Test void receptionHasNoCrmNumberAndUsesTheCustomerWithoutRequiringAnExpression() {
        var context=contexts.received(new IncomingEmailEvent("long-request-uuid","P","customer@example.test",List.of(),List.of(),"My request","Body",Map.of(),Instant.now()));
        assertEquals("",context.get("ticketNumber"));assertEquals("customer@example.test",renderer.render("{{recipient}}",context));
        assertEquals("Hemos recibido tu solicitud",catalog.require("EMAIL_RECEIVED").subject());
    }
    @Test void rejectsVariablesUnavailableBeforeCrmCreationAndDiscardsRawRemoteErrors() {
        assertThrows(IllegalArgumentException.class,()->catalog.validate("EMAIL_RECEIVED","Caso {{ticketNumber}}"));
        assertThrows(IllegalArgumentException.class,()->catalog.validate("CRM_CREATED","Caso {{typo}}"));
        assertThrows(IllegalArgumentException.class,()->catalog.validate("CRM_CREATED","Caso {{ticketNumber"));
        var context=contexts.system(new SystemEvent("id","P","customer@example.test","CRM_ERROR","Safe public message",Map.of("error","remote-echoed-key"),Instant.now()));
        assertFalse(context.toString().contains("remote-echoed-key"));
    }
    @Test void previewUsesTheSameRenderingAndFictitiousDataWithoutSendingAnything() {
        var service=new NotificationPreviewService(contexts,catalog,renderer);var event=catalog.require("CRM_CREATED");
        var preview=service.preview(new NotificationAdminService.TemplateRequest("P",event.code(),"MAIL","{{recipient}}",event.subject(),event.body(),true));
        assertEquals("cliente@ejemplo.test",preview.recipient());assertEquals("Caso #12345 registrado",preview.subject());assertTrue(preview.body().contains("12345"));
    }
}
