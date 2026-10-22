package com.agnosticrouter.decision.domain.service;

import com.agnosticrouter.decision.domain.model.KnowledgeModels;

import com.agnosticrouter.contracts.DecisionValue;
import java.util.*;

/** Creates context/hints for a single unresolved dimension. */
public class DecisionContextBuilder {
    public Map<String, Object> buildState(
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


}
