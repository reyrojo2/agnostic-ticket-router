package com.agnosticrouter.knowledge.application.port;

import com.agnosticrouter.knowledge.domain.model.DecisionOption;

/** Persistence contract owned by this microservice's application layer. */
public interface DecisionOptionStore {
    java.util.List<DecisionOption> findByDimensionIdAndActiveTrueOrderByPriorityAsc(Long dimensionId);
    java.util.List<DecisionOption> findByDimensionIdOrderByPriorityAsc(Long dimensionId);
    java.util.Optional<DecisionOption> findById(Long id);
    <S extends DecisionOption> S save(S entity);
    void deleteById(Long id);
}
