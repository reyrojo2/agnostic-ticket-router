package com.agnosticrouter.crm.infrastructure.persistence;

import com.agnosticrouter.crm.application.port.CrmOperationStore;

import com.agnosticrouter.crm.domain.model.CrmOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CrmOperationRepository extends JpaRepository<CrmOperation, Long>, CrmOperationStore {
    List<CrmOperation> findByIntegrationIdOrderByCodeAsc(Long integrationId);
    Optional<CrmOperation> findByIntegrationIdAndCodeAndActiveTrue(Long integrationId, String code);
}
