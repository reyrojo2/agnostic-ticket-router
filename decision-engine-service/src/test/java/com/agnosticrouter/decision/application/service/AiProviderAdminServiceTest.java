package com.agnosticrouter.decision.application.service;

import com.agnosticrouter.decision.application.model.AiConnection;
import com.agnosticrouter.decision.application.port.*;
import com.agnosticrouter.decision.domain.model.AiProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiProviderAdminServiceTest {
    private final AiProviderStore store = mock(AiProviderStore.class);
    private final AiSecretProtector secrets = mock(AiSecretProtector.class);
    private final AiEnvironmentSettings environment = () -> new AiConnection("http://localhost:8000", "/v1/systemone", "", "es", "NONE", "", null);
    private final AiProviderAdminService service = new AiProviderAdminService(store, secrets, environment);
    private AiProvider existing(boolean active) { return new AiProvider(7L, "Proveedor", "https://ai.test", "/v1/systemone", "model", "es", "BEARER", "X-Api-Key", "ciphertext", true, active); }
    private AiProviderAdminService.SaveProvider request(String url, String key, boolean enabled) { return new AiProviderAdminService.SaveProvider("Proveedor", url, "/v1/systemone", "model", "es", "BEARER", "X-Api-Key", key, false, enabled); }
    @Test void encryptsNewKeyAndNeverReturnsItInViews() throws Exception {
        when(secrets.encrypt("plain-test-key")).thenReturn("ciphertext");
        when(store.save(any())).thenAnswer(call -> call.getArgument(0));
        var view = service.save(null, request("https://ai.test", "plain-test-key", true));
        var saved = org.mockito.ArgumentCaptor.forClass(AiProvider.class); verify(store).save(saved.capture());
        assertEquals("ciphertext", saved.getValue().encryptedApiKey()); assertTrue(view.apiKeyConfigured());
        String json = new ObjectMapper().writeValueAsString(view);
        assertFalse(json.contains("plain-test-key")); assertFalse(json.contains("ciphertext"));
        assertFalse(request("https://ai.test", "plain-test-key", true).toString().contains("plain-test-key"));
    }
    @Test void blankKeyOnEditPreservesStoredCredential() {
        when(store.findById(7L)).thenReturn(Optional.of(existing(false)));
        when(store.save(any())).thenAnswer(call -> call.getArgument(0));
        service.save(7L, request("https://ai.test", "", true));
        var saved = org.mockito.ArgumentCaptor.forClass(AiProvider.class); verify(store).save(saved.capture());
        assertEquals("ciphertext", saved.getValue().encryptedApiKey()); verifyNoInteractions(secrets);
    }
    @Test void defaultProviderCannotBeDisabledOrDeletedAccidentally() {
        when(store.findById(7L)).thenReturn(Optional.of(existing(true)));
        assertThrows(IllegalArgumentException.class, () -> service.save(7L, request("https://ai.test", "", false)));
        assertThrows(IllegalArgumentException.class, () -> service.delete(7L)); verify(store, never()).delete(any());
    }
    @Test void rejectsCredentialUrlsAndRelativeUrls() {
        assertThrows(IllegalArgumentException.class, () -> service.save(null, request("https://user:secret@ai.test", "key", true)));
        assertThrows(IllegalArgumentException.class, () -> service.save(null, request("/not-a-host", "key", true)));
        verify(store, never()).save(any());
    }
}
