package com.agnosticrouter.notification.application.service;

import com.agnosticrouter.contracts.IncomingEmailEvent;
import com.agnosticrouter.contracts.SystemEvent;
import org.springframework.stereotype.Component;
import java.util.*;

/** Builds message variables from event contracts; never accesses CRM storage or generates a case number. */
@Component
public class NotificationContextFactory {
    public Map<String,Object> received(IncomingEmailEvent event) {
        var context=base(event.requestId(),event.profileCode(),event.from());
        context.put("from",event.from());context.put("subject",event.subject());context.put("type","EMAIL_RECEIVED");
        context.put("occurredAt",event.receivedAt());
        return context;
    }
    public Map<String,Object> system(SystemEvent event) {
        var context=base(event.requestId(),event.profileCode(),event.recipient());
        Map<String,Object> data=new LinkedHashMap<>();
        if(event.data()!=null) for(String key:List.of("externalTicketId","externalTicketNumber","ticketReference","integrationCode","operationCode","httpStatus")) {
            if(event.data().get(key)!=null)data.put(key,event.data().get(key));
        }
        String number=text(data.get("externalTicketNumber"));
        String reference=number.isBlank()?text(data.get("ticketReference")):number;
        // Existing producers can still identify their real CRM record; a request UUID is never a ticket reference.
        if(reference.isBlank()&&!data.containsKey("ticketReference"))reference=text(data.get("externalTicketId"));
        context.put("ticketNumber",number);context.put("ticketReference",reference);context.put("data",data);
        context.put("type",event.type());context.put("message",event.message());context.put("occurredAt",event.occurredAt());
        context.put("confirmationSubject",reference.isBlank()?"Solicitud registrada en el CRM":"Caso #"+reference+" registrado");
        context.put("confirmationMessage",reference.isBlank()
                ?"Tu solicitud fue registrada correctamente. Puedes contactarnos para consultar su seguimiento."
                :"Tu solicitud fue registrada correctamente. Tu número de caso es "+reference+". Conserva este número para consultar el estado de tu solicitud.");
        return context;
    }
    private Map<String,Object> base(String id,String profile,String recipient) {
        Map<String,Object> context=new LinkedHashMap<>();
        context.put("requestId",id);context.put("profileCode",profile);context.put("recipient",recipient);
        context.put("ticketNumber","");context.put("ticketReference","");context.put("data",Map.of());
        return context;
    }
    private String text(Object value){return value==null?"":value.toString().trim();}
}
