package com.agnosticrouter.crm.application.service;

import com.agnosticrouter.crm.application.port.*;
import com.agnosticrouter.crm.domain.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CrmCompatibilityServiceTest {
    @Test void reportsUnknownCustomFieldAndMissingExternalTeam() {
        CrmOperationStore operations=mock(CrmOperationStore.class);
        CrmFieldMappingStore fields=mock(CrmFieldMappingStore.class);
        CrmAssignmentMappingStore assignments=mock(CrmAssignmentMappingStore.class);
        CrmCatalogPort remote=mock(CrmCatalogPort.class);
        CrmIntegration integration=new CrmIntegration();integration.setCode("ESPOCRM_LOCAL");
        CrmOperation operation=new CrmOperation();operation.setId(4L);
        operation.setIntegration(integration);operation.setPathTemplate("/api/v1/Case");
        operation.setBodyTemplate("{\"name\":\"{{subject}}\"}");
        when(operations.findById(4L)).thenReturn(Optional.of(operation));
        when(remote.fields(integration,"Case")).thenReturn(Map.of("name", Map.of("type","varchar")));
        CrmFieldMapping mapped=new CrmFieldMapping();mapped.setTargetField("cMissing");mapped.setValueMapJson("{}");mapped.setActive(true);
        when(fields.findByOperationIdOrderByIdAsc(4L)).thenReturn(List.of(mapped));
        CrmAssignmentMapping ref=new CrmAssignmentMapping();ref.setExternalId("absent-team");ref.setAssignmentKind("TEAM");ref.setActive(true);
        when(assignments.findByOperationIdOrderByIdAsc(4L)).thenReturn(List.of(ref));
        when(remote.recordExists(integration,"Team","absent-team")).thenReturn(false);
        var result=new CrmCompatibilityService(operations,fields,assignments,remote,new ObjectMapper()).validate(4L);
        assertFalse(result.compatible());
        assertTrue(result.errors().stream().anyMatch(x->x.contains("cMissing")));
        assertTrue(result.errors().stream().anyMatch(x->x.contains("absent-team")));
    }
}
