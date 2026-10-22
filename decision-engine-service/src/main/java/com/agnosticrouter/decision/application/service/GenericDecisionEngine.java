package com.agnosticrouter.decision.application.service;

import com.agnosticrouter.decision.domain.model.KnowledgeModels;
import com.agnosticrouter.decision.domain.service.RuleEvaluator;
import com.agnosticrouter.decision.domain.service.HardRuleDecisionResolver;
import com.agnosticrouter.decision.domain.service.DimensionDependencyPolicy;
import com.agnosticrouter.decision.domain.service.DecisionContextBuilder;
import com.agnosticrouter.decision.application.port.KnowledgeProfileProvider;
import com.agnosticrouter.decision.application.model.AiFailureExplanation;

import com.agnosticrouter.contracts.DecisionValue;
import com.agnosticrouter.contracts.IncomingEmailEvent;
import com.agnosticrouter.contracts.StandardTicketEvent;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

@Service
public class GenericDecisionEngine {
    private static final Logger log = LoggerFactory.getLogger(GenericDecisionEngine.class);
    private final KnowledgeProfileProvider knowledge;
    private final RuleEvaluator rules;
    private final AiDimensionEvaluator aiDimensions;
    private final DecisionContextBuilder contexts;
    private final DimensionDependencyPolicy dependencies;
    private final HardRuleDecisionResolver hardRuleDecisions;

    public GenericDecisionEngine(KnowledgeProfileProvider knowledge, RuleEvaluator rules, HardRuleDecisionResolver hardRuleDecisions,
                                 AiDimensionEvaluator aiDimensions, DecisionContextBuilder contexts,
                                 DimensionDependencyPolicy dependencies) {
        this.knowledge = knowledge;
        this.rules = rules;
        this.hardRuleDecisions = hardRuleDecisions;
        this.aiDimensions = aiDimensions;
        this.contexts = contexts;
        this.dependencies = dependencies;
    }

    public StandardTicketEvent evaluate(IncomingEmailEvent email) {
        KnowledgeModels.ProfileConfig profile = Objects.requireNonNull(
                knowledge.getProfile(email.profileCode()),
                "El servicio de conocimiento no devolvió el perfil " + email.profileCode()
        );

        Map<String, DecisionValue> decisions = new LinkedHashMap<>();
        boolean review = false;
        AiFailureExplanation aiFailure = null;
        String raw = (email.subject() + "\n" + email.body()).trim();

        Map<String, Object> baseState = new LinkedHashMap<>();
        baseState.put("subject", email.subject());
        baseState.put("body", email.body());
        baseState.put("from", email.from());
        baseState.put("to", email.to());
        baseState.put("cc", email.cc());
        if (email.metadata() != null && !email.metadata().isEmpty()) {
            baseState.put("metadata", email.metadata());
        }

        for (KnowledgeModels.DimensionConfig dimension : profile.dimensions()) {
            if (!dependencies.satisfied(
                    dimension.dependsOnDimensionCode(),
                    dimension.dependsOnOptionCode(),
                    decisions
            )) {
                continue;
            }

            List<KnowledgeModels.OptionConfig> eligible = dimension.options().stream()
                    .filter(option -> dependencies.satisfied(
                            option.dependsOnDimensionCode(),
                            option.dependsOnOptionCode(),
                            decisions
                    ))
                    .toList();

            if (eligible.isEmpty() && !"NOUL".equalsIgnoreCase(dimension.primitiveType())) {
                decisions.put(dimension.code(), DecisionDiagnostics.explain(dimension, unavailable(dimension),
                        "NO_ELIGIBLE_OPTIONS", "No hay respuestas habilitadas que cumplan las condiciones de esta decisión.", null));
                if (dimension.required()) review = true;
                continue;
            }

            Optional<DecisionValue> hardDecision = hardRuleDecisions.resolve(raw, dimension);
            if (hardDecision.isPresent()) {
                decisions.put(dimension.code(), DecisionDiagnostics.explain(dimension, hardDecision.get(),
                        "RULE_MATCH", "Una regla determinística resolvió esta decisión.", null));
                continue;
            }

            List<KnowledgeModels.RuleConfig> softHints = rules.resolveSoft(raw, dimension.rules());
            Map<String, Object> state = contexts.buildState(baseState, decisions, softHints);
            // HARD was already resolved above; only unresolved dimensions may use AI.
            // SOFT rules are passed as hints, never as authoritative classifications.
            DecisionValue value;
            if (aiFailure != null) {
                value = DecisionDiagnostics.explain(dimension, unavailable(dimension), "AI_SKIPPED_AFTER_FAILURE",
                        "No se volvió a consultar la IA porque falló antes en este correo. " + aiFailure.message(), aiFailure.status());
            } else {
                try {
                    value = aiDimensions.evaluate(dimension, eligible, state, softHints);
                } catch (RuntimeException ex) {
                    // Avoid redelivery/credit loops if the provider is unavailable.
                    // Healthy requests still evaluate every unresolved dimension independently.
                    aiFailure = AiFailureExplanation.from(ex);
                    log.warn("AI unavailable for requestId={} dimension={} code={} httpStatus={}",
                            email.requestId(), dimension.code(), aiFailure.code(), aiFailure.status());
                    value = DecisionDiagnostics.explain(dimension, unavailable(dimension), aiFailure.code(), aiFailure.message(), aiFailure.status());
                }
            }
            decisions.put(dimension.code(), value);

            if (dimension.required() && !value.accepted()) {
                review = true;
            }
        }

        return new StandardTicketEvent(
                email.requestId(),
                profile.code(),
                profile.version(),
                email.from(),
                email.to(),
                email.cc(),
                email.subject(),
                email.body(),
                email.metadata() == null ? Map.of() : email.metadata(),
                decisions,
                review,
                Instant.now()
        );
    }

    private DecisionValue unavailable(KnowledgeModels.DimensionConfig dimension) {
        return new DecisionValue(dimension.code(), "REVIEW_REQUIRED", "UNAVAILABLE",
                0.0, null, null, null, false, Map.of(), null);
    }

}
