package com.agnosticrouter.crm.service;

import com.agnosticrouter.contracts.StandardTicketEvent;

/** Infrastructure-independent port for executing a configured CRM operation. */
public interface CrmExecutionPort {
    CrmExecutionResult execute(CrmExecutionPlan plan, StandardTicketEvent ticket);
}
