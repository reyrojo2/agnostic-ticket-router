package com.agnosticrouter.decision;

import com.agnosticrouter.contracts.DecisionValue;
import java.util.*;
import org.springframework.stereotype.Component;

/** Adapts AI questions and responses to domain decisions. No Rabbit or JDBC. */
@Component
public class AiDimensionEvaluator {
    private final DecisionAiClient ai;

    public AiDimensionEvaluator(DecisionAiClient ai) {
        this.ai = ai;
    }

    public DecisionValue evaluate(
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
                ai.decide(state, dimension.code(), question, dimension.autoMinProbability()),
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


}
