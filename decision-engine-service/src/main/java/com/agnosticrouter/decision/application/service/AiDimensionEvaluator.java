package com.agnosticrouter.decision.application.service;

import com.agnosticrouter.decision.application.port.DecisionAiClient;
import com.agnosticrouter.decision.domain.model.KnowledgeModels;
import com.agnosticrouter.decision.application.model.SystemOneModels;
import com.agnosticrouter.decision.application.model.InvalidAiResponseException;

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

        SystemOneModels.Response response = ai.decide(state, dimension.code(), question, dimension.autoMinProbability());
        if (response == null || response.answers() == null) throw new InvalidAiResponseException();
        SystemOneModels.Answer answer = response.answers().get(dimension.code());
        if (answer == null) throw new InvalidAiResponseException();

        boolean truncated = response.usage() != null && (Boolean.TRUE.equals(response.usage().truncated())
                || response.usage().truncatedQuestions() != null && response.usage().truncatedQuestions().contains(dimension.code()));
        boolean lowConfidence = Boolean.TRUE.equals(answer.lowConfidence()) || answer.abstention() != null && !answer.abstention().isBlank();
        double confidence = Optional.ofNullable(answer.answerConfidence()).orElse(Optional.ofNullable(answer.confidence()).orElse(0.0));
        String hintCodes = softHints.isEmpty()
                ? null
                : String.join(",", softHints.stream().map(KnowledgeModels.RuleConfig::code).toList());

        if ("NOUL".equals(type)) {
            Double yesValue = answer.noul();
            if (yesValue == null || !Double.isFinite(yesValue) || yesValue < 0 || yesValue > 1) throw new InvalidAiResponseException();
            double yes = yesValue;
            String value = yes >= .5 ? "YES" : "NO";
            double probability = Math.max(yes, 1 - yes);
            boolean accepted = probability >= dimension.autoMinProbability() && !truncated && !lowConfidence;
            return explain(dimension, new DecisionValue(
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
            ), truncated, lowConfidence);
        }

        if ("SCORE".equals(type)) {
            if (answer.score() == null || !Double.isFinite(answer.score())) throw new InvalidAiResponseException();
            double score = answer.score();
            double probability = confidence;
            boolean accepted = probability >= dimension.autoMinProbability() && !truncated && !lowConfidence;
            return explain(dimension, new DecisionValue(
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
            ), truncated, lowConfidence);
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
                .orElse(confidence);

        String second = ranking.size() > 1 ? ranking.get(1).getKey() : null;
        double secondProbability = ranking.size() > 1 ? ranking.get(1).getValue() : 0.0;
        double margin = firstProbability - secondProbability;

        boolean knownOption = first != null && options.stream().anyMatch(option -> option.code().equals(first));
        boolean accepted = knownOption
                && firstProbability >= dimension.autoMinProbability()
                && margin >= dimension.minMargin()
                && !truncated
                && !lowConfidence;

        DecisionValue value = new DecisionValue(
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
        if (!knownOption) return DecisionDiagnostics.explain(dimension, value, "AI_INVALID_OPTION",
                "La IA no seleccionó una respuesta habilitada para esta decisión.", null);
        return explain(dimension, value, truncated, lowConfidence);
    }

    private DecisionValue explain(KnowledgeModels.DimensionConfig dimension, DecisionValue value,
                                  boolean truncated, boolean lowConfidence) {
        String code, message;
        if (truncated) { code = "AI_TRUNCATED"; message = "El proveedor recortó la información y la respuesta necesita revisión."; }
        else if (value.accepted()) { code = "AI_ACCEPTED"; message = "La respuesta IA cumple los criterios de aceptación."; }
        else if (value.probability() < dimension.autoMinProbability()) { code = "AI_LOW_CONFIDENCE"; message = "La confianza de la respuesta no alcanza el mínimo configurado."; }
        else if (value.margin() != null && value.margin() < dimension.minMargin()) { code = "AI_LOW_MARGIN"; message = "Las dos primeras alternativas tienen probabilidades demasiado cercanas."; }
        else if (lowConfidence) { code = "AI_ABSTAINED"; message = "El proveedor señaló incertidumbre o se abstuvo de decidir."; }
        else { code = "AI_INVALID_RESPONSE"; message = "La respuesta no cumple los criterios de aceptación."; }
        return DecisionDiagnostics.explain(dimension, value, code, message, null);
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
