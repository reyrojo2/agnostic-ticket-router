package com.agnosticrouter.crm.application.port;

import com.agnosticrouter.crm.domain.model.CrmOperation;

/** Persistence contract owned by this microservice's application layer. */
public interface CrmOperationStore {
    java.util.List<CrmOperation> findAll();
    java.util.Optional<CrmOperation> findById(Long id);
    java.util.List<CrmOperation> findByIntegrationIdOrderByCodeAsc(Long integrationId);
    java.util.Optional<CrmOperation> findByIntegrationIdAndCodeAndActiveTrue(Long integrationId, String code);
    <S extends CrmOperation> S save(S entity);
    void deleteById(Long id);
}
