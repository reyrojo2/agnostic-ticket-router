package com.agnosticrouter.crm.application.model;

import com.agnosticrouter.crm.domain.model.CrmIntegration;
import com.agnosticrouter.crm.domain.model.CrmOperation;
import com.agnosticrouter.crm.domain.model.CrmRoute;

public record CrmExecutionPlan(
        CrmRoute route,
        CrmIntegration integration,
        CrmOperation operation
) {}
