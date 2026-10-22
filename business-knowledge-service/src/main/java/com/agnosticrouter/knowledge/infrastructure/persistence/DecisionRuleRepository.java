package com.agnosticrouter.knowledge.infrastructure.persistence;

import com.agnosticrouter.knowledge.application.port.DecisionRuleStore;

import com.agnosticrouter.knowledge.domain.model.DecisionRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecisionRuleRepository extends JpaRepository<DecisionRule, Long>, DecisionRuleStore {
    List<DecisionRule> findByDimensionIdAndActiveTrueOrderByPriorityAsc(Long dimensionId);
    List<DecisionRule> findByDimensionIdOrderByPriorityAsc(Long dimensionId);
}
