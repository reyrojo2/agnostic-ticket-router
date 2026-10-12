package com.agnosticrouter.decision;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class KnowledgeClient {
 private final RestClient client; public KnowledgeClient(@Qualifier("knowledgeClient") RestClient c){client=c;}
 public KnowledgeModels.ProfileConfig getProfile(String code){ return client.get().uri("/api/v1/runtime/profiles/{code}",code).retrieve().body(KnowledgeModels.ProfileConfig.class); }
}
