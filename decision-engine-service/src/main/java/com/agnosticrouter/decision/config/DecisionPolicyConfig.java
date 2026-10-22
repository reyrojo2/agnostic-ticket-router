package com.agnosticrouter.decision.config;

import com.agnosticrouter.decision.domain.service.DecisionContextBuilder;
import com.agnosticrouter.decision.domain.service.DimensionDependencyPolicy;
import com.agnosticrouter.decision.domain.service.HardRuleDecisionResolver;
import com.agnosticrouter.decision.domain.service.RuleEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires domain policies without making the policies depend on Spring. */
@Configuration
public class DecisionPolicyConfig {
    @Bean
    public RuleEvaluator ruleEvaluator() {
        return new RuleEvaluator();
    }

    @Bean
    public HardRuleDecisionResolver hardRuleDecisionResolver(RuleEvaluator evaluator) {
        return new HardRuleDecisionResolver(evaluator);
    }

    @Bean
    public DimensionDependencyPolicy dimensionDependencyPolicy() {
        return new DimensionDependencyPolicy();
    }

    @Bean
    public DecisionContextBuilder decisionContextBuilder() {
        return new DecisionContextBuilder();
    }
}
