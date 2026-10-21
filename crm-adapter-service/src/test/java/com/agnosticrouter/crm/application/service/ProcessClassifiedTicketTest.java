package com.agnosticrouter.crm.application.service;

import com.agnosticrouter.crm.application.port.CrmExecutionPort;
import com.agnosticrouter.crm.application.port.CrmOutcomePublisher;

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
        new ProcessClassifiedTicket(routes,executor,output).process(ticket);
        verifyNoInteractions(routes,executor);
        verify(output).publish(eq(ticket),eq("REVIEW_REQUIRED"),anyString(),anyMap());
    }
}
