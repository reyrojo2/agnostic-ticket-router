package com.agnosticrouter.decision.application.service;

import com.agnosticrouter.decision.application.model.*;
import com.agnosticrouter.decision.application.port.*;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class AiProviderProbeService {
    private final AiProviderStore providers;
    private final AiProviderResolver resolver;
    private final SystemOneTransport transport;
    public AiProviderProbeService(AiProviderStore providers, AiProviderResolver resolver, SystemOneTransport transport) { this.providers = providers; this.resolver = resolver; this.transport = transport; }
    public ProbeResult test(Long id) {
        var provider = providers.findById(id).orElseThrow(() -> new NoSuchElementException("Proveedor no encontrado."));
        long start = System.nanoTime();
        try {
            var response = transport.decide(resolver.connection(provider), Map.of("text", "Prueba de conexión al motor de decisiones."), "connection_test",
                    new SystemOneModels.Question("noul", "¿El texto indica que esta es una prueba de conexión?", null), .5);
            var answer = response == null || response.answers() == null ? null : response.answers().get("connection_test");
            boolean valid = answer != null && answer.noul() != null && Double.isFinite(answer.noul()) && answer.noul() >= 0 && answer.noul() <= 1;
            return result(valid, valid ? "El proveedor respondió al contrato SystemOne." : "El servidor respondió, pero no devolvió una respuesta SystemOne válida.", start);
        } catch (RuntimeException ex) {
            var explanation = AiFailureExplanation.from(ex);
            String status = explanation.status() == null ? "" : " (HTTP " + explanation.status() + ").";
            return result(false, explanation.message() + status, start);
        }
    }
    private ProbeResult result(boolean success, String message, long start) { return new ProbeResult(success, message, (System.nanoTime() - start) / 1_000_000); }
    public record ProbeResult(boolean success, String message, long durationMs) {}
}
