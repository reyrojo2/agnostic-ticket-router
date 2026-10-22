package com.agnosticrouter.decision.domain.service;

import com.agnosticrouter.decision.domain.model.KnowledgeModels;

import com.agnosticrouter.contracts.DecisionValue;
import java.util.Map;
import java.util.Optional;

/**
 * Pure, deterministic classification policy. A conclusive HARD rule always
 * wins; an ambiguous or missing HARD rule delegates the dimension to AI.
 */
public class HardRuleDecisionResolver {
    private final RuleEvaluator evaluator;
    public HardRuleDecisionResolver(RuleEvaluator evaluator) { this.evaluator = evaluator; }

    public Optional<DecisionValue> resolve(String text, KnowledgeModels.DimensionConfig dimension) {
        return evaluator.resolveHard(text, dimension.rules()).map(rule -> new DecisionValue(
                dimension.code(), rule.targetOptionCode(), "RULE", 1.0, null,
                0.0, 1.0, true, Map.of(rule.targetOptionCode(), 1.0), rule.code()
        ));
    }
}
