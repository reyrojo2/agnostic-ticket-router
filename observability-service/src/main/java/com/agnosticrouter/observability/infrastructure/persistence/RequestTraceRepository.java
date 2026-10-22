package com.agnosticrouter.observability.infrastructure.persistence;

import com.agnosticrouter.contracts.*;
import com.agnosticrouter.observability.application.port.RequestTraceReader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Repository
public class RequestTraceRepository implements RequestTraceReader {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public RequestTraceRepository(JdbcTemplate jdbc,ObjectMapper mapper){this.jdbc=jdbc;this.mapper=mapper;}

    public void received(IncomingEmailEvent event){
        String source = String.valueOf(event.metadata()==null?"api":event.metadata().getOrDefault("source","api"));
        jdbc.update("""
            INSERT INTO observability.request_trace(request_id,profile_code,source)
            VALUES (?,?,?)
            ON CONFLICT(request_id) DO UPDATE SET
              profile_code=COALESCE(observability.request_trace.profile_code,EXCLUDED.profile_code),
              source=EXCLUDED.source
            """,event.requestId(),event.profileCode(),source.substring(0,Math.min(40,source.length())));
    }
    public void classified(StandardTicketEvent event){
        Map<String,Object> values=new LinkedHashMap<>();
        int rules=0,ai=0,rejected=0;
        for(var entry:event.decisions().entrySet()){
            DecisionValue decision=entry.getValue();
            Map<String,Object> compact=new LinkedHashMap<>();
            compact.put("value",decision.value());
            compact.put("source",decision.source());
            compact.put("accepted",decision.accepted());
            compact.put("confidence",decision.probability());
            compact.put("margin",decision.margin());
            compact.put("secondOption",decision.secondOption());
            compact.put("secondConfidence",decision.secondProbability());
            compact.put("matchedRule",decision.matchedRule());
            compact.put("diagnostic",decision.diagnostic());
            values.put(entry.getKey(),compact);
            if("RULE".equals(decision.source()))rules++;
            if("AI".equals(decision.source()))ai++;
            if(!decision.accepted())rejected++;
        }
        String json;
        try{json=mapper.writeValueAsString(values);}catch(JsonProcessingException ex){throw new IllegalStateException("Invalid decision JSON",ex);}
        jdbc.update("""
          INSERT INTO observability.request_trace(request_id,profile_code,state,classified_at,
            deterministic_count,ai_count,rejected_count,requires_review,decisions)
          VALUES (?,?,?,now(),?,?,?,?,?::jsonb)
          ON CONFLICT(request_id) DO UPDATE SET
            state=CASE WHEN observability.request_trace.state IN ('CRM_CREATED','CRM_ERROR','REVIEW_REQUIRED')
                 THEN observability.request_trace.state ELSE EXCLUDED.state END,
            classified_at=EXCLUDED.classified_at,
            deterministic_count=EXCLUDED.deterministic_count,
            ai_count=EXCLUDED.ai_count,rejected_count=EXCLUDED.rejected_count,
            requires_review=observability.request_trace.requires_review OR EXCLUDED.requires_review,
            decisions=EXCLUDED.decisions
          """,event.requestId(),event.profileCode(),
          event.requiresHumanReview()?"REVIEW_REQUIRED":"CLASSIFIED",rules,ai,rejected,event.requiresHumanReview(),json);
    }
    public void system(SystemEvent event){
        if(!List.of("CRM_CREATED","CRM_ERROR","REVIEW_REQUIRED").contains(event.type()))return;
        Object id=event.data()==null?null:event.data().get("externalTicketId");
        String external=id==null?null:id.toString();
        if(external!=null && external.length()>120)external=external.substring(0,120);
        Object rawNumber=event.data()==null?null:event.data().get("externalTicketNumber");
        String externalNumber=rawNumber==null?null:rawNumber.toString();
        if(externalNumber!=null && externalNumber.length()>150)externalNumber=externalNumber.substring(0,150);
        // Event error bodies may echo credentials. Keep only the public outcome and numeric HTTP status.
        String message = switch (event.type()) {
            case "CRM_CREATED" -> "El CRM confirmó la creación del caso.";
            case "CRM_ERROR" -> "El adaptador no pudo completar el envío al CRM. Revisa la integración y su historial.";
            default -> "La solicitud se detuvo antes de enviarse al CRM porque requiere revisión.";
        };
        Object rawStatus = event.data() == null ? null : event.data().get("httpStatus");
        Integer status = rawStatus instanceof Number number && number.intValue() >= 100 && number.intValue() <= 599 ? number.intValue() : null;
        jdbc.update("""
          INSERT INTO observability.request_trace(request_id,profile_code,state,crm_at,crm_external_id,requires_review,outcome_message,crm_http_status,crm_external_number)
          VALUES (?,?,?,now(),?,?,?,?,?)
          ON CONFLICT(request_id) DO UPDATE SET
            state=EXCLUDED.state,crm_at=EXCLUDED.crm_at,crm_external_id=EXCLUDED.crm_external_id,
            requires_review=observability.request_trace.requires_review OR EXCLUDED.requires_review,
            outcome_message=EXCLUDED.outcome_message,crm_http_status=EXCLUDED.crm_http_status,crm_external_number=EXCLUDED.crm_external_number
          """,event.requestId(),event.profileCode(),event.type(),external,"REVIEW_REQUIRED".equals(event.type()),message,status,externalNumber);
    }
    public List<Map<String,Object>> recent(LocalDate start, String timezone){
        return jdbc.query("""
            SELECT request_id, received_at, profile_code, source, state, deterministic_count,
                   ai_count,rejected_count,requires_review,crm_external_id,decisions,outcome_message,crm_http_status,crm_external_number
            FROM observability.request_trace
            WHERE (received_at AT TIME ZONE ?)::date >= ?
            ORDER BY received_at DESC
            """,(rs,row)->{
                Map<String,Object> value=new LinkedHashMap<>();
                value.put("requestId",rs.getString("request_id"));
                value.put("at",rs.getTimestamp("received_at").toInstant());
                value.put("profile",rs.getString("profile_code"));
                value.put("source",rs.getString("source"));
                value.put("state",rs.getString("state"));
                value.put("rules",rs.getInt("deterministic_count"));
                value.put("ai",rs.getInt("ai_count"));
                value.put("rejected",rs.getInt("rejected_count"));
                value.put("review",rs.getBoolean("requires_review"));
                value.put("externalId",rs.getString("crm_external_id"));
                value.put("externalNumber",rs.getString("crm_external_number"));
                value.put("outcomeMessage",rs.getString("outcome_message"));
                value.put("crmHttpStatus",rs.getObject("crm_http_status",Integer.class));
                try{value.put("decisions",mapper.readValue(rs.getString("decisions"),Map.class));}
                catch(JsonProcessingException ex){throw new IllegalStateException("Invalid stored JSON",ex);}
                return value;
            },timezone,java.sql.Date.valueOf(start));
    }
}
