package com.agnosticrouter.knowledge.infrastructure.persistence;

import com.agnosticrouter.knowledge.application.port.DecisionOptionStore;

import com.agnosticrouter.knowledge.domain.model.DecisionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecisionOptionRepository extends JpaRepository<DecisionOption, Long>, DecisionOptionStore {
    List<DecisionOption> findByDimensionIdAndActiveTrueOrderByPriorityAsc(Long dimensionId);
    List<DecisionOption> findByDimensionIdOrderByPriorityAsc(Long dimensionId);
}
