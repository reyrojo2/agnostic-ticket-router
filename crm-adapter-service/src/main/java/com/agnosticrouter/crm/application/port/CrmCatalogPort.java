package com.agnosticrouter.crm.application.port;

import com.agnosticrouter.crm.domain.model.CrmIntegration;
import java.util.List;
import java.util.Map;

/** External metadata and reference operations; no CRM-specific HTTP in application services. */
public interface CrmCatalogPort {
    Map<String, Map<String, Object>> fields(CrmIntegration integration, String entityType);
    List<ExternalRecord> records(CrmIntegration integration, String entityType);
    ExternalRecord ensureTeam(CrmIntegration integration, String name);
    boolean recordExists(CrmIntegration integration, String entityType, String id);

    record ExternalRecord(String id, String name, boolean created) {}
}
