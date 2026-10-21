package com.agnosticrouter.crm.infrastructure.persistence;

import com.agnosticrouter.crm.application.port.CrmIntegrationStore;

import com.agnosticrouter.crm.domain.model.CrmIntegration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrmIntegrationRepository extends JpaRepository<CrmIntegration, Long>, CrmIntegrationStore {
    Optional<CrmIntegration> findByCodeAndActiveTrue(String code);
}
