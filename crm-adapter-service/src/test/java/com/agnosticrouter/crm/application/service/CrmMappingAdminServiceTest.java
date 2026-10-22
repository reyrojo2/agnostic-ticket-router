package com.agnosticrouter.crm.application.service;

import com.agnosticrouter.crm.application.port.*;
import com.agnosticrouter.crm.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CrmMappingAdminServiceTest {
    @Test
    void reusedTeamUpdatesExistingAssignmentRatherThanCreatingADuplicate() {
        CrmOperationStore operations = mock(CrmOperationStore.class);
        CrmAssignmentMappingStore assignments = mock(CrmAssignmentMappingStore.class);
        CrmCatalogPort catalog = mock(CrmCatalogPort.class);
        CrmIntegration integration = new CrmIntegration();
        CrmOperation operation = new CrmOperation();
        operation.setId(2L);
        operation.setIntegration(integration);
        when(operations.findById(2L)).thenReturn(Optional.of(operation));
        when(catalog.ensureTeam(integration, "Back Office"))
                .thenReturn(new CrmCatalogPort.ExternalRecord("team-existing", "Back Office", false));
        CrmAssignmentMapping existing = new CrmAssignmentMapping();
        existing.setId(10L);
        existing.setOperation(operation);
        existing.setDimensionCode("DESTINO");
        existing.setOptionCode("BACKOFFICE");
        existing.setAssignmentKind("TEAM");
        when(assignments.findByOperationIdOrderByIdAsc(2L)).thenReturn(List.of(existing));
        when(assignments.findById(10L)).thenReturn(Optional.of(existing));
        when(assignments.save(existing)).thenReturn(existing);

        var service = new CrmMappingAdminService(operations, mock(CrmFieldMappingStore.class), assignments, catalog);
        var result = service.ensureTeam(2L,
                new CrmMappingAdminService.TeamRequest("DESTINO", "BACKOFFICE", "Back Office"));

        assertFalse(result.created());
        assertEquals(10L, result.mappingId());
        assertEquals("team-existing", existing.getExternalId());
        verify(assignments).save(existing);
    }

    @Test
    void invalidDimensionCannotCreateAnExternalTeam() {
        CrmCatalogPort catalog = mock(CrmCatalogPort.class);
        var service = new CrmMappingAdminService(mock(CrmOperationStore.class),
                mock(CrmFieldMappingStore.class), mock(CrmAssignmentMappingStore.class), catalog);
        assertThrows(IllegalArgumentException.class, () -> service.ensureTeam(2L,
                new CrmMappingAdminService.TeamRequest("invalid dimension", "BACKOFFICE", "Back Office")));
        verifyNoInteractions(catalog);
    }
}
