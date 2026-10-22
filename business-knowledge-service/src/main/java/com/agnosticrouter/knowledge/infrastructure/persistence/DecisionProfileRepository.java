package com.agnosticrouter.knowledge.infrastructure.persistence;

import com.agnosticrouter.knowledge.application.port.DecisionProfileStore;
import com.agnosticrouter.knowledge.domain.model.DecisionProfile;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DecisionProfileRepository extends JpaRepository<DecisionProfile,Long>, DecisionProfileStore { java.util.Optional<DecisionProfile> findByCodeAndActiveTrue(String code); }
