package com.agnosticrouter.knowledge.repo;
import com.agnosticrouter.knowledge.domain.DecisionProfile;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DecisionProfileRepository extends JpaRepository<DecisionProfile,Long> { java.util.Optional<DecisionProfile> findByCodeAndActiveTrue(String code); }
