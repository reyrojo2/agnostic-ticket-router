package com.agnosticrouter.decision.application.service;

import com.agnosticrouter.decision.application.port.ClassifiedTicketPublisher;

import com.agnosticrouter.contracts.IncomingEmailEvent;
import com.agnosticrouter.contracts.StandardTicketEvent;

import com.agnosticrouter.decision.application.port.ProcessedDecisionStore;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessIncomingEmailTest {
    private IncomingEmailEvent message() {
        return new IncomingEmailEvent("mail-1","TELCO_DEMO","user@example.org",
                List.of(),List.of(),"Subject","Body",Map.of(),Instant.now());
    }
    @Test void alreadyCompletedRequestNeverEvaluatesAiOrPublishes() {
        ProcessedDecisionStore store=mock(ProcessedDecisionStore.class);
        GenericDecisionEngine engine=mock(GenericDecisionEngine.class);
        ClassifiedTicketPublisher out=mock(ClassifiedTicketPublisher.class);
        when(store.isProcessed("mail-1")).thenReturn(true);
        assertFalse(new ProcessIncomingEmail(store,engine,out).process(message()));
        verifyNoInteractions(engine,out);
        verify(store,never()).markProcessed("mail-1");
    }
    @Test void marksProcessedOnlyAfterPublication() {
        ProcessedDecisionStore store=mock(ProcessedDecisionStore.class);
        GenericDecisionEngine engine=mock(GenericDecisionEngine.class);
        ClassifiedTicketPublisher out=mock(ClassifiedTicketPublisher.class);
        StandardTicketEvent ticket=mock(StandardTicketEvent.class);
        when(engine.evaluate(any())).thenReturn(ticket);
        assertTrue(new ProcessIncomingEmail(store,engine,out).process(message()));
        var order=inOrder(out,store);
        order.verify(out).publish(ticket);
        order.verify(store).markProcessed("mail-1");
    }
}
