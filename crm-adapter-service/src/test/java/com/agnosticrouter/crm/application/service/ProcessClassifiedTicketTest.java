package com.agnosticrouter.crm.application.service;

import com.agnosticrouter.crm.application.port.CrmExecutionPort;
import com.agnosticrouter.crm.application.port.CrmOutcomePublisher;
import com.agnosticrouter.crm.application.model.*;
import com.agnosticrouter.crm.domain.model.*;

import com.agnosticrouter.contracts.StandardTicketEvent;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ProcessClassifiedTicketTest {
    @Test void humanReviewNeverContactsExternalCrm() {
        CrmRouter routes=mock(CrmRouter.class);
        CrmExecutionPort executor=mock(CrmExecutionPort.class);
        CrmOutcomePublisher output=mock(CrmOutcomePublisher.class);
        StandardTicketEvent ticket=mock(StandardTicketEvent.class);
        when(ticket.requiresHumanReview()).thenReturn(true);
        when(ticket.decisions()).thenReturn(Map.of());
        new ProcessClassifiedTicket(routes,executor,output,mock(CrmAuditService.class)).process(ticket);
        verifyNoInteractions(routes,executor);
        verify(output).publish(eq(ticket),eq("REVIEW_REQUIRED"),anyString(),anyMap());
    }
    @Test void publishesBothTheCrmRecordIdAndTheCustomerCaseNumber() {
        var router=mock(CrmRouter.class);var executor=mock(CrmExecutionPort.class);var output=mock(CrmOutcomePublisher.class);var audit=mock(CrmAuditService.class);var ticket=mock(StandardTicketEvent.class);
        var integration=new CrmIntegration();integration.setCode("ESPOCRM");var operation=new CrmOperation();operation.setCode("CREATE_CASE");operation.setExternalNumberPath("number");
        var plan=new CrmExecutionPlan(null,integration,operation);
        when(router.resolve(ticket)).thenReturn(plan);when(executor.execute(plan,ticket)).thenReturn(new CrmExecutionResult(true,200,"technical-record-id","{}",1,null,"5"));
        new ProcessClassifiedTicket(router,executor,output,audit).process(ticket);
        verify(output).publish(eq(ticket),eq("CRM_CREATED"),anyString(),argThat(data->"5".equals(data.get("externalTicketNumber"))&&"5".equals(data.get("ticketReference"))&&"technical-record-id".equals(data.get("externalTicketId"))));
    }
}
