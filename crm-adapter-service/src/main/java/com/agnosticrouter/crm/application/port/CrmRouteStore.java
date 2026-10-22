package com.agnosticrouter.crm.application.port;

import com.agnosticrouter.crm.domain.model.CrmRoute;

/** Persistence contract owned by this microservice's application layer. */
public interface CrmRouteStore {
    java.util.List<CrmRoute> findAll();
    java.util.Optional<CrmRoute> findById(Long id);
    java.util.List<CrmRoute> findByProfileCodeAndActiveTrueOrderByPriorityAsc(String profileCode);
    <S extends CrmRoute> S save(S entity);
    void deleteById(Long id);
}
