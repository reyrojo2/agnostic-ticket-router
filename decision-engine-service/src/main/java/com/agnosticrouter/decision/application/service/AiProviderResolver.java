package com.agnosticrouter.decision.application.service;

import com.agnosticrouter.decision.application.model.AiConnection;
import com.agnosticrouter.decision.application.port.*;
import com.agnosticrouter.decision.domain.model.AiProvider;
import org.springframework.stereotype.Service;

@Service
public class AiProviderResolver {
    private final AiProviderStore providers;
    private final AiSecretProtector secrets;
    private final AiEnvironmentSettings environment;
    public AiProviderResolver(AiProviderStore providers, AiSecretProtector secrets, AiEnvironmentSettings environment) {
        this.providers = providers; this.secrets = secrets; this.environment = environment;
    }
    public AiConnection current() { return providers.findDefault().map(this::connection).orElseGet(environment::connection); }
    public AiConnection connection(AiProvider provider) {
        if (!provider.enabled()) throw new IllegalArgumentException("El proveedor está deshabilitado.");
        return new AiConnection(provider.baseUrl(), provider.endpointPath(), provider.model(), provider.language(),
                provider.authType(), provider.apiKeyHeader(), provider.encryptedApiKey() == null ? null : secrets.decrypt(provider.encryptedApiKey()));
    }
}
