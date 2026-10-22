package com.agnosticrouter.contracts;

import java.util.Map;

public record DecisionValue(
        String dimensionCode,
        String value,
        String source,
        Double probability,
        String secondOption,
        Double secondProbability,
        Double margin,
        boolean accepted,
        Map<String, Double> probabilities,
        String matchedRule,
        DecisionDiagnostic diagnostic
) {
    /** Keeps producers using the original event contract source-compatible. */
    public DecisionValue(String dimensionCode, String value, String source, Double probability,
                         String secondOption, Double secondProbability, Double margin, boolean accepted,
                         Map<String, Double> probabilities, String matchedRule) {
        this(dimensionCode, value, source, probability, secondOption, secondProbability, margin,
                accepted, probabilities, matchedRule, null);
    }

    public DecisionValue withDiagnostic(DecisionDiagnostic explanation) {
        return new DecisionValue(dimensionCode, value, source, probability, secondOption,
                secondProbability, margin, accepted, probabilities, matchedRule, explanation);
    }
}
