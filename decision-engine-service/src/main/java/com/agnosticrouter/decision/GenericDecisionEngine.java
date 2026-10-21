package com.agnosticrouter.decision;

import com.agnosticrouter.contracts.DecisionValue;
import com.agnosticrouter.contracts.IncomingEmailEvent;
import com.agnosticrouter.contracts.StandardTicketEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class GenericDecisionEngine {
    private final KnowledgeClient knowledge;
    private final DecisionAiClient ai;
    private final RuleEvaluator rules;
    private final AiUsageGuard guard;

    public GenericDecisionEngine(KnowledgeClient knowledge, DecisionAiClient ai, RuleEvaluator rules, AiUsageGuard guard) {
        this.knowledge = knowledge;
        this.ai = ai;
        this.rules = rules;
        this.guard = guard;
    }

    public StandardTicketEvent evaluate(IncomingEmailEvent email) {
        KnowledgeModels.ProfileConfig profile = Objects.requireNonNull(
                knowledge.getProfile(email.profileCode()),
                "El servicio de conocimiento no devolvió el perfil " + email.profileCode()
        );

        Map<String, DecisionValue> decisions = new LinkedHashMap<>();
        boolean review = false;
        int aiCalls = 0;
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
            if (!dependencySatisfied(
                    dimension.dependsOnDimensionCode(),
                    dimension.dependsOnOptionCode(),
                    decisions
            )) {
                continue;
            }

            List<KnowledgeModels.OptionConfig> eligible = dimension.options().stream()
                    .filter(option -> dependencySatisfied(
                            option.dependsOnDimensionCode(),
                            option.dependsOnOptionCode(),
                            decisions
                    ))
                    .toList();

            if (eligible.isEmpty() && !"NOUL".equalsIgnoreCase(dimension.primitiveType())) {
                continue;
            }

            Optional<KnowledgeModels.RuleConfig> hard = rules.resolveHard(raw, dimension.rules());
            if (hard.isPresent()) {
                KnowledgeModels.RuleConfig rule = hard.get();
                decisions.put(
                        dimension.code(),
                        new DecisionValue(
                                dimension.code(),
                                rule.targetOptionCode(),
                                "RULE",
                                1.0,
                                null,
                                0.0,
                                1.0,
                                true,
                                Map.of(rule.targetOptionCode(), 1.0),
                                rule.code()
                        )
                );
                continue;
            }

            List<KnowledgeModels.RuleConfig> softHints = rules.resolveSoft(raw, dimension.rules());
            Map<String, Object> state = buildState(baseState, decisions, softHints);
            DecisionValue value;
            if (!guard.enabled() || aiCalls >= guard.maxCallsPerMessage()) {
                value = unresolved(dimension);
            } else {
                aiCalls++;
                try {
                    value = evaluateWithLaya(dimension, eligible, state, softHints);
                } catch (RuntimeException ex) {
                    // API outages, quota exhaustion, and invalid responses must never loop through RabbitMQ.
                    value = unresolved(dimension);
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

    private DecisionValue unresolved(KnowledgeModels.DimensionConfig d) {
        return new DecisionValue(d.code(), "REVIEW_REQUIRED", "UNAVAILABLE", 0.0, null,
            null, null, false, Map.of(), null);
    }

    private Map<String, Object> buildState(
            Map<String, Object> baseState,
            Map<String, DecisionValue> decisions,
            List<KnowledgeModels.RuleConfig> softHints
    ) {
        Map<String, Object> state = new LinkedHashMap<>(baseState);

        if (!decisions.isEmpty()) {
            Map<String, String> resolved = new LinkedHashMap<>();
            decisions.forEach((code, value) -> resolved.put(code, value.value()));
            state.put("resolved_decisions", resolved);
        }

        if (!softHints.isEmpty()) {
            state.put(
                    "business_hints",
                    softHints.stream()
                            .map(rule -> Map.of(
                                    "rule", rule.code(),
                                    "suggested_option", rule.targetOptionCode()
                            ))
                            .toList()
            );
        }

        return state;
    }

    private DecisionValue evaluateWithLaya(
            KnowledgeModels.DimensionConfig dimension,
            List<KnowledgeModels.OptionConfig> options,
            Map<String, Object> state,
            List<KnowledgeModels.RuleConfig> softHints
    ) {
        String type = dimension.primitiveType().toUpperCase(Locale.ROOT);
        Object criteria;

        if ("SCORE".equals(type)) {
            criteria = options.stream()
                    .sorted(Comparator.comparingInt(KnowledgeModels.OptionConfig::priority))
                    .map(this::criteriaText)
                    .toList();
        } else if ("NOUL".equals(type)) {
            criteria = null;
        } else {
            Map<String, String> map = new LinkedHashMap<>();
            options.stream()
                    .sorted(Comparator.comparingInt(KnowledgeModels.OptionConfig::priority))
                    .forEach(option -> map.put(option.code(), criteriaText(option)));
            criteria = map;
        }

        SystemOneModels.Question question = new SystemOneModels.Question(
                type.toLowerCase(Locale.ROOT),
                dimension.instructions(),
                criteria
        );

        SystemOneModels.Response response = Objects.requireNonNull(
                guard.decide(ai, state, dimension.code(), question, dimension.autoMinProbability()),
                "Laya no devolvió respuesta"
        );

        SystemOneModels.Answer answer = Objects.requireNonNull(
                response.answers().get(dimension.code()),
                "Laya no respondió la dimensión " + dimension.code()
        );

        boolean truncated = response.usage() != null && Boolean.TRUE.equals(response.usage().truncated());
        String hintCodes = softHints.isEmpty()
                ? null
                : String.join(",", softHints.stream().map(KnowledgeModels.RuleConfig::code).toList());

        if ("NOUL".equals(type)) {
            double yes = Optional.ofNullable(answer.noul()).orElse(0.0);
            String value = yes >= .5 ? "YES" : "NO";
            double probability = Math.max(yes, 1 - yes);
            boolean accepted = probability >= dimension.autoMinProbability() && !truncated;
            return new DecisionValue(
                    dimension.code(),
                    value,
                    "AI",
                    probability,
                    null,
                    null,
                    null,
                    accepted,
                    Map.of("YES", yes, "NO", 1 - yes),
                    hintCodes
            );
        }

        if ("SCORE".equals(type)) {
            double score = Optional.ofNullable(answer.score()).orElse(0.0);
            double probability = Optional.ofNullable(answer.answerConfidence()).orElse(0.0);
            boolean accepted = probability >= dimension.autoMinProbability() && !truncated;
            return new DecisionValue(
                    dimension.code(),
                    Double.toString(score),
                    "AI",
                    probability,
                    null,
                    null,
                    null,
                    accepted,
                    Optional.ofNullable(answer.probabilities()).orElse(Map.of()),
                    hintCodes
            );
        }

        Map<String, Double> probabilities = Optional.ofNullable(answer.probabilities()).orElse(Map.of());
        List<Map.Entry<String, Double>> ranking = probabilities.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .toList();

        String first = answer.choice() != null
                ? answer.choice()
                : (ranking.isEmpty() ? null : ranking.get(0).getKey());

        double firstProbability = ranking.stream()
                .filter(entry -> Objects.equals(entry.getKey(), first))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(Optional.ofNullable(answer.answerConfidence()).orElse(0.0));

        String second = ranking.size() > 1 ? ranking.get(1).getKey() : null;
        double secondProbability = ranking.size() > 1 ? ranking.get(1).getValue() : 0.0;
        double margin = firstProbability - secondProbability;

        boolean accepted = first != null
                && firstProbability >= dimension.autoMinProbability()
                && margin >= dimension.minMargin()
                && !truncated
                && !Boolean.TRUE.equals(answer.lowConfidence());

        return new DecisionValue(
                dimension.code(),
                first,
                "AI",
                firstProbability,
                second,
                secondProbability,
                margin,
                accepted,
                probabilities,
                hintCodes
        );
    }

    private String criteriaText(KnowledgeModels.OptionConfig option) {
        StringBuilder text = new StringBuilder(option.description());
        if (option.includeContext() != null && !option.includeContext().isBlank()) {
            text.append(" Incluye: ").append(option.includeContext());
        }
        if (option.excludeContext() != null && !option.excludeContext().isBlank()) {
            text.append(" Excluir: ").append(option.excludeContext());
        }
        return text.toString();
    }

    private boolean dependencySatisfied(
            String dimension,
            String option,
            Map<String, DecisionValue> decisions
    ) {
        if (dimension == null || dimension.isBlank()) {
            return true;
        }
        DecisionValue parent = decisions.get(dimension);
        return parent != null && (option == null || option.isBlank() || option.equals(parent.value()));
    }
}
