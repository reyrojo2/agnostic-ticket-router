package com.agnosticrouter.decision.api.controller;

import com.agnosticrouter.decision.application.service.*;
import com.agnosticrouter.decision.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AiProviderController.class, properties = "app.security.jwt-secret=test-secret-at-least-thirty-two-bytes-long")
@Import(SecurityConfig.class)
class AiProviderControllerTest {
    @Autowired MockMvc http;
    @MockitoBean AiProviderAdminService admin;
    @MockitoBean AiProviderProbeService probes;
    @Test void anonymousRequestsAreRejected() throws Exception { http.perform(get("/api/v1/admin/ai/providers")).andExpect(status().isUnauthorized()); verifyNoInteractions(admin); }
    @Test void nonAdminTokensCannotReadProviderConfiguration() throws Exception { http.perform(get("/api/v1/admin/ai/providers").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))).andExpect(status().isForbidden()); verifyNoInteractions(admin); }
    @Test void adminResponsesExposeOnlyCredentialPresence() throws Exception {
        when(admin.list()).thenReturn(List.of(new AiProviderAdminService.ProviderView(1L, "Provider", "https://test.invalid", "/v1/systemone", "model", "es", "BEARER", "X-Api-Key", true, true, false)));
        http.perform(get("/api/v1/admin/ai/providers").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].apiKeyConfigured").value(true)).andExpect(jsonPath("$[0].apiKey").doesNotExist()).andExpect(jsonPath("$[0].encryptedApiKey").doesNotExist());
    }
}
