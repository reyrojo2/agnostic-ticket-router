package com.agnosticrouter.decision.application.port;

import com.agnosticrouter.decision.domain.model.AiProvider;
import java.util.List;
import java.util.Optional;

public interface AiProviderStore {
    List<AiProvider> findAll();
    Optional<AiProvider> findById(Long id);
    Optional<AiProvider> findDefault();
    AiProvider save(AiProvider provider);
    void selectDefault(Long id);
    void clearDefault();
    void delete(Long id);
}
