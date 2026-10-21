package com.agnosticrouter.decision.infrastructure.client;

import com.agnosticrouter.decision.domain.model.KnowledgeModels;
import com.agnosticrouter.decision.application.port.KnowledgeProfileProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class KnowledgeClient implements KnowledgeProfileProvider {
 private final RestClient client; public KnowledgeClient(@Qualifier("knowledgeRestClient") RestClient c){client=c;}
 public KnowledgeModels.ProfileConfig getProfile(String code){ return client.get().uri("/api/v1/runtime/profiles/{code}",code).retrieve().body(KnowledgeModels.ProfileConfig.class); }
}
