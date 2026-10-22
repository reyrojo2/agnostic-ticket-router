package com.agnosticrouter.decision.application.port;

import com.agnosticrouter.decision.domain.model.KnowledgeModels;

/** Fetches a versioned, runtime decision profile without exposing HTTP transport. */
public interface KnowledgeProfileProvider {
    KnowledgeModels.ProfileConfig getProfile(String code);
}
