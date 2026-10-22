package com.agnosticrouter.decision.application.service;

import com.agnosticrouter.decision.application.model.SystemOneModels;
import com.agnosticrouter.decision.application.port.DecisionAiClient;
import com.agnosticrouter.decision.application.port.SystemOneTransport;
import org.springframework.stereotype.Service;

/** Runtime selection is resolved on each call; changing the provider needs no restart. */
@Service
public class ConfiguredDecisionAiClient implements DecisionAiClient {
    private final AiProviderResolver resolver;
    private final SystemOneTransport transport;
    public ConfiguredDecisionAiClient(AiProviderResolver resolver, SystemOneTransport transport) { this.resolver = resolver; this.transport = transport; }
    @Override public SystemOneModels.Response decide(Object state, String questionId, SystemOneModels.Question question, double minConfidence) {
        return transport.decide(resolver.current(), state, questionId, question, minConfidence);
    }
}
