package com.agnosticrouter.knowledge.infrastructure.persistence;

import com.agnosticrouter.knowledge.application.port.DecisionDimensionStore;

import com.agnosticrouter.knowledge.domain.model.DecisionDimension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecisionDimensionRepository extends JpaRepository<DecisionDimension, Long>, DecisionDimensionStore {
    List<DecisionDimension> findByProfileIdAndActiveTrueOrderByExecutionOrderAsc(Long profileId);
    List<DecisionDimension> findByProfileIdOrderByExecutionOrderAsc(Long profileId);
}
