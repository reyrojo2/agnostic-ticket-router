package com.agnosticrouter.crm.application.port;

import com.agnosticrouter.crm.application.model.CrmExecutionPlan;
import com.agnosticrouter.crm.application.model.CrmExecutionResult;

import com.agnosticrouter.contracts.StandardTicketEvent;

/** Infrastructure-independent port for executing a configured CRM operation. */
public interface CrmExecutionPort {
    CrmExecutionResult execute(CrmExecutionPlan plan, StandardTicketEvent ticket);
}
