package com.agnosticrouter.decision.application.service;

import com.agnosticrouter.contracts.DecisionDiagnostic;
import com.agnosticrouter.contracts.DecisionValue;
import com.agnosticrouter.decision.domain.model.KnowledgeModels;

/** Builds explanations from the configuration used for this evaluation, not from later edits. */
public final class DecisionDiagnostics {
    private DecisionDiagnostics() {}

    public static DecisionValue explain(KnowledgeModels.DimensionConfig dimension, DecisionValue value,
                                        String code, String message, Integer status) {
        String name = dimension.options().stream().filter(option -> option.code().equals(value.value()))
                .map(KnowledgeModels.OptionConfig::name).findFirst().orElse(value.value());
        return value.withDiagnostic(new DecisionDiagnostic(code, message, dimension.name(), name,
                dimension.required(), dimension.autoMinProbability(),
                "CHOICE".equalsIgnoreCase(dimension.primitiveType()) ? dimension.minMargin() : null, status));
    }
}
