package com.agnosticrouter.crm.application.port;

import com.agnosticrouter.crm.domain.model.CrmIntegration;

/** Persistence contract owned by this microservice's application layer. */
public interface CrmIntegrationStore {
    java.util.List<CrmIntegration> findAll();
    java.util.Optional<CrmIntegration> findById(Long id);
    java.util.Optional<CrmIntegration> findByCodeAndActiveTrue(String code);
    <S extends CrmIntegration> S save(S entity);
    void deleteById(Long id);
}
