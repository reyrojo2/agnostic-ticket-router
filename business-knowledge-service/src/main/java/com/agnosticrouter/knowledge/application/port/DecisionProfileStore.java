package com.agnosticrouter.knowledge.application.port;

import com.agnosticrouter.knowledge.domain.model.DecisionProfile;

/** Persistence contract owned by this microservice's application layer. */
public interface DecisionProfileStore {
    java.util.List<DecisionProfile> findAll();
    java.util.Optional<DecisionProfile> findById(Long id);
    java.util.Optional<DecisionProfile> findByCodeAndActiveTrue(String code);
    <S extends DecisionProfile> S save(S entity);
    void deleteById(Long id);
}
