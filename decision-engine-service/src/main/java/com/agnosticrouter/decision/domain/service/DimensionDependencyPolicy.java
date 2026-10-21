package com.agnosticrouter.decision.domain.service;

import com.agnosticrouter.contracts.DecisionValue;
import java.util.Map;

/** Evaluates dependent dimensions/options against already resolved values. */
public class DimensionDependencyPolicy {
    public boolean satisfied(
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
