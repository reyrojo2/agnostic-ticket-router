package com.agnosticrouter.crm.api.controller;

import com.agnosticrouter.crm.application.port.CrmCatalogPort;
import com.agnosticrouter.crm.application.port.CrmIntegrationStore;
import com.agnosticrouter.crm.application.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CrmMappingErrorHandlerTest {
    private final CrmMappingAdminService mappings = mock(CrmMappingAdminService.class);
    private final CrmCompatibilityService compatibility = mock(CrmCompatibilityService.class);
    private final MockMvc http = MockMvcBuilders.standaloneSetup(new CrmMappingController(
            mappings, mock(CrmPreviewService.class), compatibility, mock(CrmAuditService.class),
            mock(CrmCatalogPort.class), mock(CrmIntegrationStore.class)))
            .setControllerAdvice(new CrmMappingErrorHandler()).build();

    @Test
    void deletedOperationReturnsNotFound() throws Exception {
        when(compatibility.validate(2L)).thenThrow(new NoSuchElementException());
        http.perform(post("/api/v1/admin/crm/operations/2/validate"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void duplicateAssignmentReturnsConflictWithoutDatabaseDetails() throws Exception {
        when(mappings.saveAssignment(eq(2L), eq(null), any()))
                .thenThrow(new DataIntegrityViolationException("private SQL details"));
        http.perform(post("/api/v1/admin/crm/operations/2/assignments")
                        .contentType("application/json")
                        .content("{\"dimensionCode\":\"DESTINO\",\"optionCode\":\"BACKOFFICE\",\"assignmentKind\":\"TEAM\",\"externalId\":\"team-1\",\"active\":true}"))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private SQL"))));
    }
}
