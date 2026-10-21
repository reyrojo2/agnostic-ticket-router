package com.agnosticrouter.gmail.service;

import com.agnosticrouter.gmail.config.GmailSettings;
import com.agnosticrouter.gmail.repo.GmailRepository;
import com.agnosticrouter.gmail.gateway.*;
import com.agnosticrouter.gmail.security.TokenCipher;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GmailService {
 private final GmailRepository repo;
 private final GoogleMailClient google;
 private final IntakePublisher publisher;
 private final TokenCipher tokens;
 private final MailParser parser;
 private final GmailSettings cfg;
 private final AtomicBoolean busy=new AtomicBoolean();

 public GmailService(GmailRepository repo,GoogleMailClient google,IntakePublisher publisher,
       TokenCipher tokens,MailParser parser,GmailSettings cfg){
  this.repo=repo;this.google=google;this.publisher=publisher;this.tokens=tokens;this.parser=parser;this.cfg=cfg;
 }
 public Map<String,Object> status(){
  GmailRepository.Account a=repo.account();
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
 public Map<String,String> authorize(){
  if(!cfg.configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Configura Gmail OAuth y cifrado");
  String state=Base64.getUrlEncoder().withoutPadding().encodeToString(random(32));
  repo.state(state);
  String url=UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
    .queryParam("client_id",cfg.clientId())
    .queryParam("redirect_uri",cfg.redirectUri())
    .queryParam("response_type","code")
    .queryParam("scope","https://www.googleapis.com/auth/gmail.readonly")
    .queryParam("access_type","offline")
    .queryParam("prompt","consent")
    .queryParam("state",state)
    .queryParam("include_granted_scopes","false").build().encode().toUriString();
  return Map.of("url",url);
 }
 private byte[] random(int size){byte[] b=new byte[size];new java.security.SecureRandom().nextBytes(b);return b;}
 public String callback(String code,String state,String error){
  if(error!=null||code==null||state==null)return "/gmail?error=consent";
  if(!repo.consumeState(state))return "/gmail?error=state";
  try{
   Map<String,Object> data=google.exchangeCode(code);
   String refresh=(String)data.get("refresh_token"),access=(String)data.get("access_token");
   if(refresh==null||access==null)throw new IllegalArgumentException("OAuth sin refresh_token");
   String email=(String)google.profile(access).get("emailAddress");
   Number lifetime=(Number)data.getOrDefault("expires_in",3600);
   repo.saveAccount(email,tokens.encrypt(refresh),tokens.encrypt(access),Instant.now().getEpochSecond()+lifetime.longValue());
   return "/gmail?connected=1";
  }catch(RuntimeException ex){return "/gmail?error=oauth";}
 }
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
  try{
   GmailRepository.Account a=repo.account();
   if(a==null||!a.enabled())return Map.of("status","disabled","processed",0);
   String token=access(a);
   String page=null;
   for(int p=0;p<3;p++){
    Map<String,Object> listing=google.list(token,a.query(),page);
    List<Map<String,Object>> messages=(List<Map<String,Object>>)listing.getOrDefault("messages",List.of());
    for(int i=messages.size()-1;i>=0;i--){
     String id=String.valueOf(messages.get(i).get("id"));
     if(repo.processed(a.email(),id))continue;
     String raw=(String)google.message(token,id).get("raw");
     if(raw==null)continue;
     MailParser.Mail msg=parser.parse(raw);
     if(msg.from()==null||!msg.from().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
           ||msg.subject().isBlank()||msg.body().isBlank())continue;
     String requestId=publisher.publish(a.profileCode(),msg.from(),msg.subject(),msg.body(),id);
     repo.markProcessed(a.email(),id,requestId);processed++;
    }
    page=(String)listing.get("nextPageToken");if(page==null||page.isBlank())break;
   }
   repo.synced();return Map.of("status","ok","processed",processed);
  }catch(Exception ex){
   repo.failed("Falló sincronización ("+ex.getClass().getSimpleName()+"); verifica credenciales y conexión.");
   return Map.of("status","error","processed",processed,"detail",ex.getClass().getSimpleName());
  }finally{busy.set(false);}
 }
 private String access(GmailRepository.Account a){
  long now=Instant.now().getEpochSecond();
  if(a.accessToken()!=null&&a.expiresAt()>now+60)return tokens.decrypt(a.accessToken());
  String refresh=tokens.decrypt(a.refreshToken());
  Map<String,Object> response=google.refresh(refresh);
  String access=(String)response.get("access_token");
  Number duration=(Number)response.getOrDefault("expires_in",3600);
  repo.access(tokens.encrypt(access),now+duration.longValue());
  return access;
 }
}
