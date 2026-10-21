package com.agnosticrouter.decision;

import com.agnosticrouter.contracts.DecisionValue;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Evaluates dependent dimensions/options against already resolved values. */
@Component
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
