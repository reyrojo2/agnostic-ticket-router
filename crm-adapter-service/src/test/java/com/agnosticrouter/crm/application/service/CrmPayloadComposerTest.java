package com.agnosticrouter.crm.application.service;

import com.agnosticrouter.contracts.DecisionValue;
import com.agnosticrouter.contracts.StandardTicketEvent;
import com.agnosticrouter.crm.application.port.CrmAssignmentMappingStore;
import com.agnosticrouter.crm.application.port.CrmFieldMappingStore;
import com.agnosticrouter.crm.domain.model.CrmAssignmentMapping;
import com.agnosticrouter.crm.domain.model.CrmFieldMapping;
import com.agnosticrouter.crm.domain.model.CrmOperation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CrmPayloadComposerTest {
    private final ObjectMapper jackson = new ObjectMapper().findAndRegisterModules();
    private final CrmFieldMappingStore fields = mock(CrmFieldMappingStore.class);
    private final CrmAssignmentMappingStore assignments = mock(CrmAssignmentMappingStore.class);
    private final CrmPayloadComposer composer = new CrmPayloadComposer(new TemplateRenderer(jackson), fields, assignments, jackson);
    private final CrmOperation operation = new CrmOperation();

    @BeforeEach void init() {
        operation.setId(4L);
        operation.setBodyTemplate("{\"name\":\"{{subject}}\",\"description\":\"{{classificationSummary}}\"}");
        when(fields.findByOperationIdOrderByIdAsc(4L)).thenReturn(List.of());
        when(assignments.findByOperationIdOrderByIdAsc(4L)).thenReturn(List.of());
    }
    @Test void mapsMultipleDimensionsAndAssignsTeamsAndOwner() throws Exception {
        CrmFieldMapping field = new CrmFieldMapping();
        field.setDimensionCode("TIPO_GESTION"); field.setTargetField("cTipologia");
        field.setValueMapJson("{\"CAMBIO_RUC\":\"Cambio de RUC\"}"); field.setActive(true);
        when(fields.findByOperationIdOrderByIdAsc(4L)).thenReturn(List.of(field));
        when(assignments.findByOperationIdOrderByIdAsc(4L)).thenReturn(List.of(
                assignment("DESTINO", "BACKOFFICE", "TEAM", "team-123"),
                assignment("DESTINO", "BACKOFFICE", "USER", "user-456")));
        var rendered = composer.compose(operation, ticket());
        JsonNode body = jackson.readTree(rendered.json());
        assertEquals("Cambio de RUC", body.path("cTipologia").asText());
        assertEquals("team-123", body.path("teamsIds").get(0).asText());
        assertEquals("user-456", body.path("assignedUserId").asText());
        assertTrue(body.path("description").asText().contains("DESTINO: BACKOFFICE"));
        assertTrue(rendered.mappedFields().contains("cTipologia"));
    }
    @Test void failsClosedWhenARequiredDimensionIsNotClassified() {
        CrmFieldMapping required = new CrmFieldMapping();
        required.setDimensionCode("MISSING");required.setTargetField("cMissing");
        required.setRequired(true);required.setActive(true);
        when(fields.findByOperationIdOrderByIdAsc(4L)).thenReturn(List.of(required));
        assertThrows(IllegalArgumentException.class, () -> composer.compose(operation, ticket()));
    }
    @Test void refusesToOverwriteReservedRecordId() {
        CrmFieldMapping invalid = new CrmFieldMapping();
        invalid.setDimensionCode("DESTINO");invalid.setTargetField("id");invalid.setActive(true);
        when(fields.findByOperationIdOrderByIdAsc(4L)).thenReturn(List.of(invalid));
        assertThrows(IllegalArgumentException.class, () -> composer.compose(operation, ticket()));
    }
    @Test void failsWhenTwoOwnersWouldBeAssigned() {
        when(assignments.findByOperationIdOrderByIdAsc(4L)).thenReturn(List.of(
                assignment("DESTINO", "BACKOFFICE", "USER", "owner-1"),
                assignment("TIPO_GESTION", "CAMBIO_RUC", "USER", "owner-2")));
        assertThrows(IllegalArgumentException.class, () -> composer.compose(operation, ticket()));
    }
    private CrmAssignmentMapping assignment(String dim,String option,String kind,String id) {
        var a = new CrmAssignmentMapping();a.setDimensionCode(dim);a.setOptionCode(option);
        a.setAssignmentKind(kind);a.setExternalId(id);a.setActive(true);return a;
    }
    private StandardTicketEvent ticket() {
        return new StandardTicketEvent("REQ-123", "TELCO_DEMO", 1,"someone@example.test",List.of(),List.of(),
                "Cambio de RUC \"especial\"","Texto del correo",Map.of(),Map.of(
                    "DESTINO",new DecisionValue("DESTINO","BACKOFFICE","RULE",null,null,null,null,true,Map.of(),null),
                    "TIPO_GESTION",new DecisionValue("TIPO_GESTION","CAMBIO_RUC","RULE",null,null,null,null,true,Map.of(),null)
                ),false,Instant.now());
    }
}
