package com.agnosticrouter.knowledge.infrastructure.persistence;

import com.agnosticrouter.knowledge.domain.DecisionDimension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecisionDimensionRepository extends JpaRepository<DecisionDimension, Long> {
    List<DecisionDimension> findByProfileIdAndActiveTrueOrderByExecutionOrderAsc(Long profileId);
    List<DecisionDimension> findByProfileIdOrderByExecutionOrderAsc(Long profileId);
}
