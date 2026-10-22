package com.agnosticrouter.observability.infrastructure.persistence;

import com.agnosticrouter.contracts.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RequestTraceRepositoryTest {
    @Test void preservesDiagnosticThresholdsAndRuleInformationInTheReadModel() throws Exception {
        var jdbc=mock(JdbcTemplate.class);var mapper=new ObjectMapper();var repo=new RequestTraceRepository(jdbc,mapper);
        var diagnostic=new DecisionDiagnostic("AI_MODEL_REQUIRED","Completa Modelo","Prioridad","REVIEW_REQUIRED",true,.8,.1,422);
        var value=new DecisionValue("PRIORIDAD","REVIEW_REQUIRED","UNAVAILABLE",0.0,null,null,null,false,Map.of(),null,diagnostic);
        repo.classified(new StandardTicketEvent("request","PROFILE",1,"from@example.org",List.of(),List.of(),"Subject","Body",Map.of(),Map.of("PRIORIDAD",value),true,Instant.now()));
        var json=ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(anyString(),eq("request"),eq("PROFILE"),eq("REVIEW_REQUIRED"),eq(0),eq(0),eq(1),eq(true),json.capture());
        var stored=mapper.readTree(json.getValue()).path("PRIORIDAD").path("diagnostic");
        assertEquals("AI_MODEL_REQUIRED",stored.path("code").asText());assertEquals(.8,stored.path("minimumConfidence").asDouble());assertEquals(422,stored.path("providerStatus").asInt());
    }
    @Test void crmTelemetryDiscardsRemoteErrorsThatCouldContainCredentials() {
        var jdbc=mock(JdbcTemplate.class);var repo=new RequestTraceRepository(jdbc,new ObjectMapper());
        repo.system(new SystemEvent("request","PROFILE","recipient","CRM_ERROR","unsafe-secret",Map.of("httpStatus",401,"error","unsafe-secret"),Instant.now()));
        var message=ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(anyString(),eq("request"),eq("PROFILE"),eq("CRM_ERROR"),isNull(),eq(false),message.capture(),eq(401),isNull());
        assertFalse(message.getValue().contains("unsafe-secret"));assertTrue(message.getValue().contains("integración"));
    }
    @Test void originalEventWithoutDiagnosticStillDeserializes() throws Exception {
        var value=new ObjectMapper().readValue("{\"dimensionCode\":\"AREA\",\"value\":\"SOPORTE\",\"source\":\"RULE\",\"probability\":1.0,\"accepted\":true,\"probabilities\":{}}",DecisionValue.class);
        assertTrue(value.accepted());assertNull(value.diagnostic());
    }
}
