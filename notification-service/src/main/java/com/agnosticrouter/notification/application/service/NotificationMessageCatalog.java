package com.agnosticrouter.notification.application.service;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Pattern;

/** Event vocabulary, recommended text and variable validation are owned by Notification Service. */
@Component
public class NotificationMessageCatalog {
    private static final Pattern TOKEN=Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");
    private static final List<Variable> COMMON=List.of(new Variable("recipient","Correo del cliente"),new Variable("profileCode","Identificador del perfil"),new Variable("requestId","Referencia interna de seguimiento"));
    public List<EventDefinition> events(){return List.of(
            new EventDefinition("EMAIL_RECEIVED","Al recibir el correo","Confirma la recepción. Todavía no existe un número de caso CRM.","Hemos recibido tu solicitud","Hemos recibido tu solicitud \"{{subject}}\". La estamos evaluando.",variables(new Variable("subject","Asunto del correo"),new Variable("from","Correo del remitente"))),
            new EventDefinition("CRM_CREATED","Cuando el CRM confirma el caso","Incluye el número real asignado por el CRM.","{{confirmationSubject}}","{{confirmationMessage}}",variables(new Variable("ticketReference","Referencia del caso CRM"),new Variable("ticketNumber","Número de caso CRM"),new Variable("confirmationSubject","Asunto con la referencia real"),new Variable("confirmationMessage","Confirmación con la referencia real"),new Variable("data.externalTicketId","ID técnico del registro CRM"),new Variable("data.externalTicketNumber","Número devuelto por el CRM"),new Variable("data.ticketReference","Referencia devuelta por el CRM"),new Variable("message","Mensaje del evento"))),
            new EventDefinition("REVIEW_REQUIRED","Cuando necesita revisión","Informa que la solicitud necesita validación antes de crear un caso.","Tu solicitud necesita una revisión","Tu solicitud requiere una validación adicional antes de registrarse en el CRM. Nuestro equipo la revisará.",variables(new Variable("message","Mensaje del evento"))),
            new EventDefinition("CRM_ERROR","Si falla el registro en el CRM","Informa de una incidencia sin afirmar que ya existe un caso.","Estamos revisando el registro de tu solicitud","Recibimos tu solicitud, pero no pudimos completar su registro automáticamente. Nuestro equipo revisará la incidencia.",variables(new Variable("message","Mensaje del evento")))
    );}
    public EventDefinition require(String event){return events().stream().filter(e->e.code().equals(event)).findFirst().orElseThrow(()->new IllegalArgumentException("Selecciona un momento de notificación válido."));}
    public void validate(String event,String... texts){
        var allowed=new HashSet<>(require(event).variables().stream().map(Variable::key).toList());
        // These existing technical variables remain available without exposing provider error bodies.
        allowed.addAll(List.of("type","occurredAt","data.integrationCode","data.operationCode","data.httpStatus"));
        for(String text:texts){
            if(text==null)continue;
            var matcher=TOKEN.matcher(text);
            while(matcher.find())if(!allowed.contains(matcher.group(1)))throw new IllegalArgumentException("La variable "+matcher.group(1)+" no está disponible para este momento. Usa Insertar dato.");
            String remainder=TOKEN.matcher(text).replaceAll("");
            if(remainder.contains("{{")||remainder.contains("}}"))throw new IllegalArgumentException("Hay un dato dinámico incompleto. Usa Insertar dato para añadirlo.");
        }
    }
    private List<Variable> variables(Variable... specific){var result=new ArrayList<>(COMMON);result.addAll(List.of(specific));return List.copyOf(result);}
    public record Variable(String key,String label){}
    public record EventDefinition(String code,String label,String description,String subject,String body,List<Variable> variables){}
}
