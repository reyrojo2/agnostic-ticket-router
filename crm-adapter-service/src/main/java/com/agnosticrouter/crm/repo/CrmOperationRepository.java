package com.agnosticrouter.crm.repo;

import com.agnosticrouter.crm.domain.CrmOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CrmOperationRepository extends JpaRepository<CrmOperation, Long> {
    List<CrmOperation> findByIntegrationIdOrderByCodeAsc(Long integrationId);
    Optional<CrmOperation> findByIntegrationIdAndCodeAndActiveTrue(Long integrationId, String code);
}
