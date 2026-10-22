package com.agnosticrouter.decision.application.service;

import com.agnosticrouter.decision.application.model.*;
import com.agnosticrouter.decision.application.port.*;
import com.agnosticrouter.decision.domain.model.AiProvider;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfiguredDecisionAiClientTest {
    @Test void readsTheCurrentSelectionForEachDecisionAndKeepsEnvironmentFallback() {
        AiProviderStore store = mock(AiProviderStore.class);
        AiSecretProtector secrets = mock(AiSecretProtector.class);
        SystemOneTransport transport = mock(SystemOneTransport.class);
        AiConnection environment = new AiConnection("http://legacy.test", "/v1/systemone", "old", "es", "NONE", "", null);
        AiProvider selected = new AiProvider(1L, "New", "https://new.test", "/custom/systemone", "new-model", "en", "API_KEY_HEADER", "x-custom-key", "encrypted", true, true);
        when(store.findDefault()).thenReturn(Optional.empty(), Optional.of(selected));
        when(secrets.decrypt("encrypted")).thenReturn("secret-test-key");
        var client = new ConfiguredDecisionAiClient(new AiProviderResolver(store, secrets, () -> environment), transport);
        var question = new SystemOneModels.Question("choice", "Choose", Map.of("A", "A"));
        client.decide(Map.of(), "q", question, .7); client.decide(Map.of(), "q", question, .7);
        var connections = org.mockito.ArgumentCaptor.forClass(AiConnection.class);
        verify(transport, times(2)).decide(connections.capture(), any(), eq("q"), eq(question), eq(.7));
        assertEquals("http://legacy.test", connections.getAllValues().get(0).baseUrl());
        assertEquals("https://new.test", connections.getAllValues().get(1).baseUrl());
        assertEquals("secret-test-key", connections.getValue().apiKey());
        assertFalse(connections.getValue().toString().contains("secret-test-key"));
    }
}
