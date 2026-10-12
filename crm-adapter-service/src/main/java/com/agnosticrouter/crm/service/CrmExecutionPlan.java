package com.agnosticrouter.crm.service;

import com.agnosticrouter.crm.domain.CrmIntegration;
import com.agnosticrouter.crm.domain.CrmOperation;
import com.agnosticrouter.crm.domain.CrmRoute;

public record CrmExecutionPlan(
        CrmRoute route,
        CrmIntegration integration,
        CrmOperation operation
) {}
