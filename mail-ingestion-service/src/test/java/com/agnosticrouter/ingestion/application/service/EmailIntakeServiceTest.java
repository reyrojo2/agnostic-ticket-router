package com.agnosticrouter.ingestion.application.service;


import com.agnosticrouter.ingestion.application.service.EmailIntakeService;
import com.agnosticrouter.contracts.IncomingEmailEvent;
import com.agnosticrouter.ingestion.domain.model.EmailIntakeCommand;
import com.agnosticrouter.ingestion.application.port.IncomingEmailPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailIntakeServiceTest {
    @Test
    void returnsStableRequestIdForGmailRetry() {
        IncomingEmailPublisher publisher = mock(IncomingEmailPublisher.class);
        EmailIntakeService service = new EmailIntakeService(publisher);
        EmailIntakeCommand input = new EmailIntakeCommand("TELCO_DEMO","sender@example.org",
                List.of(),List.of(),"Solicitud","Cuerpo",
                Map.of("source","gmail","gmailMessageId","gmail-123"));
        String first = service.receive(input);
        String second = service.receive(input);
        assertEquals(first,second);
        ArgumentCaptor<IncomingEmailEvent> events = ArgumentCaptor.forClass(IncomingEmailEvent.class);
        verify(publisher,times(2)).publish(events.capture());
        assertEquals(first,events.getAllValues().get(0).requestId());
        assertEquals(first,events.getAllValues().get(1).requestId());
    }

    @Test
    void generatesDifferentIdsForIndependentApiRequests() {
        IncomingEmailPublisher publisher = mock(IncomingEmailPublisher.class);
        EmailIntakeService service = new EmailIntakeService(publisher);
        EmailIntakeCommand input = new EmailIntakeCommand("TELCO_DEMO","sender@example.org",
                List.of(),List.of(),"Solicitud","Cuerpo",Map.of("source","api"));
        assertNotEquals(service.receive(input),service.receive(input));
    }
}
