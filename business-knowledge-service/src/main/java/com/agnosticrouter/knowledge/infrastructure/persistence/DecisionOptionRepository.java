package com.agnosticrouter.knowledge.infrastructure.persistence;

import com.agnosticrouter.knowledge.domain.DecisionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecisionOptionRepository extends JpaRepository<DecisionOption, Long> {
    List<DecisionOption> findByDimensionIdAndActiveTrueOrderByPriorityAsc(Long dimensionId);
    List<DecisionOption> findByDimensionIdOrderByPriorityAsc(Long dimensionId);
}
