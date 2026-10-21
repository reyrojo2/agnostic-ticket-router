package com.agnosticrouter.gmail.infrastructure.client;

import com.agnosticrouter.gmail.application.port.IntakePublisher;

import com.agnosticrouter.gmail.config.GmailSettings;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestIntakePublisher implements IntakePublisher {
 private final RestClient http;private final GmailSettings cfg;
 public RestIntakePublisher(RestClient gmailHttp,GmailSettings cfg){this.http=gmailHttp;this.cfg=cfg;}
 @Override public String publish(String profile,String from,String subject,String body,String id){
  Map<?,?> response=http.post().uri(cfg.intakeUrl())
    .body(Map.of("profileCode",profile,"from",from,"subject",subject,"body",body,
      "metadata",Map.of("source","gmail","gmailMessageId",id)))
    .retrieve().body(Map.class);
  if(response==null||response.get("requestId")==null)throw new IllegalStateException("Ingesta sin requestId");
  return response.get("requestId").toString();
 }
}
