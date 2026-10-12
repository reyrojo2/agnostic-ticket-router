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
        String matchedRule
) {}
