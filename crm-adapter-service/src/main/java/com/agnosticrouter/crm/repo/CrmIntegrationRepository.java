package com.agnosticrouter.crm.repo;

import com.agnosticrouter.crm.domain.CrmIntegration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrmIntegrationRepository extends JpaRepository<CrmIntegration, Long> {
    Optional<CrmIntegration> findByCodeAndActiveTrue(String code);
}
