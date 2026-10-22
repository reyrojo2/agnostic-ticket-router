package com.agnosticrouter.gmail.application.service;
import com.agnosticrouter.gmail.application.port.GoogleMailClient;
import com.agnosticrouter.gmail.application.port.IntakePublisher;

import com.agnosticrouter.gmail.application.port.GmailRuntimeSettings;
import com.agnosticrouter.gmail.application.port.GmailStore;
import com.agnosticrouter.gmail.application.model.GmailAccount;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GmailService {
 private final GmailStore repo;
 private final GoogleMailClient google;
 private final GmailRuntimeSettings cfg;
 private final GmailMessageProcessor messageProcessor;
 private final GmailOAuthService oauth;
 private final AtomicBoolean busy=new AtomicBoolean();

 public GmailService(GmailStore repo,GoogleMailClient google,IntakePublisher publisher,
       GmailRuntimeSettings cfg,GmailOAuthService oauth,GmailMessageProcessor messageProcessor){
  this.repo=repo;this.google=google;this.cfg=cfg;this.oauth=oauth;this.messageProcessor=messageProcessor;
 }
 public Map<String,Object> status(){
  GmailAccount a=repo.account();
  Map<String,Object> s=new LinkedHashMap<>();
  s.put("configured",cfg.configured());s.put("connected",a!=null);
  s.put("email",a==null?null:a.email());s.put("enabled",a!=null&&a.enabled());
  s.put("profileCode",a==null?"TELCO_DEMO":a.profileCode());
  s.put("query",a==null?"in:inbox is:unread":a.query());
  s.put("lastSync",a==null?null:a.lastSync());
  s.put("lastError",a==null?null:a.lastError());
  s.put("pollSeconds",Math.max(60,cfg.pollSeconds()));
  return s;
 }
 public Map<String,String> authorize(){ return oauth.authorize(); }
 public String callback(String code,String state,String error){ return oauth.callback(code,state,error); }
 public Map<String,Object> save(String profile,String query,boolean enabled){
  if(repo.account()==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Conecta Gmail antes de guardar");
  if(!profile.matches("[A-Z][A-Z0-9_]{1,79}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Perfil inválido");
  if(query==null||query.isBlank()||query.length()>240)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Filtro inválido");
  repo.settings(profile,query,enabled);return status();
 }
 public Map<String,Object> disconnect(){repo.disconnect();return Map.of("connected",false);}
 @Scheduled(fixedDelayString="${app.gmail.poll-seconds:90}000",initialDelay=90000)
 public void scheduled(){sync();}
 @SuppressWarnings("unchecked")
 public Map<String,Object> sync(){
  if(!busy.compareAndSet(false,true))return Map.of("status","busy","processed",0);
  int processed=0;
  int alreadyProcessed=0;
  int listed=0;
  try{
   GmailAccount a=repo.account();
   if(a==null||!a.enabled())return Map.of("status","disabled","processed",0);
   String token=oauth.access(a);
   String page=null;
   for(int p=0;p<3;p++){
    Map<String,Object> listing=google.list(token,a.query(),page);
    List<Map<String,Object>> messages=(List<Map<String,Object>>)listing.getOrDefault("messages",List.of());
    for(int i=messages.size()-1;i>=0;i--){
     String id=String.valueOf(messages.get(i).get("id"));
     listed++;
     if(repo.processed(a.email(),id)) { alreadyProcessed++; continue; }
     if(messageProcessor.process(a,token,id))processed++;
    }
    page=(String)listing.get("nextPageToken");if(page==null||page.isBlank())break;
   }
   repo.synced();return Map.of("status","ok","processed",processed,"alreadyProcessed",alreadyProcessed,"listed",listed);
  }catch(Exception ex){
   repo.failed("Falló sincronización ("+ex.getClass().getSimpleName()+"); verifica credenciales y conexión.");
   return Map.of("status","error","processed",processed,"alreadyProcessed",alreadyProcessed,"listed",listed,"detail",ex.getClass().getSimpleName());
  }finally{busy.set(false);}
 }
}
