package com.agnosticrouter.knowledge.application.port;

import com.agnosticrouter.knowledge.domain.model.DecisionRule;

/** Persistence contract owned by this microservice's application layer. */
public interface DecisionRuleStore {
    java.util.List<DecisionRule> findByDimensionIdAndActiveTrueOrderByPriorityAsc(Long dimensionId);
    java.util.List<DecisionRule> findByDimensionIdOrderByPriorityAsc(Long dimensionId);
    java.util.Optional<DecisionRule> findById(Long id);
    <S extends DecisionRule> S save(S entity);
    void deleteById(Long id);
}
