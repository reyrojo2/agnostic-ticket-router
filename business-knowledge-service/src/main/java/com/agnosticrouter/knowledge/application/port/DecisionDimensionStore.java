package com.agnosticrouter.knowledge.application.port;

import com.agnosticrouter.knowledge.domain.model.DecisionDimension;

/** Persistence contract owned by this microservice's application layer. */
public interface DecisionDimensionStore {
    java.util.List<DecisionDimension> findByProfileIdAndActiveTrueOrderByExecutionOrderAsc(Long profileId);
    java.util.List<DecisionDimension> findByProfileIdOrderByExecutionOrderAsc(Long profileId);
    java.util.Optional<DecisionDimension> findById(Long id);
    <S extends DecisionDimension> S save(S entity);
    void deleteById(Long id);
}
