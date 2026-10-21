package com.agnosticrouter.decision;

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
    private final KnowledgeClient knowledge;
    private final RuleEvaluator rules;
    private final AiDimensionEvaluator aiDimensions;
    private final DecisionContextBuilder contexts;
    private final DimensionDependencyPolicy dependencies;
    private final HardRuleDecisionResolver hardRuleDecisions;

    public GenericDecisionEngine(KnowledgeClient knowledge, RuleEvaluator rules, HardRuleDecisionResolver hardRuleDecisions,
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
        boolean aiUnavailableForThisMessage = false;
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
                continue;
            }

            Optional<DecisionValue> hardDecision = hardRuleDecisions.resolve(raw, dimension);
            if (hardDecision.isPresent()) {
                decisions.put(dimension.code(), hardDecision.get());
                continue;
            }

            List<KnowledgeModels.RuleConfig> softHints = rules.resolveSoft(raw, dimension.rules());
            Map<String, Object> state = contexts.buildState(baseState, decisions, softHints);
            // HARD was already resolved above; only unresolved dimensions may use AI.
            // SOFT rules are passed as hints, never as authoritative classifications.
            DecisionValue value;
            if (aiUnavailableForThisMessage) {
                value = unavailable(dimension);
            } else {
                try {
                    value = aiDimensions.evaluate(dimension, eligible, state, softHints);
                } catch (RuntimeException ex) {
                    // Avoid redelivery/credit loops if the provider is unavailable.
                    // Healthy requests still evaluate every unresolved dimension independently.
                    aiUnavailableForThisMessage = true;
                    log.warn("AI unavailable for requestId={} dimension={} ({})",
                            email.requestId(), dimension.code(), ex.getClass().getSimpleName());
                    value = unavailable(dimension);
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
