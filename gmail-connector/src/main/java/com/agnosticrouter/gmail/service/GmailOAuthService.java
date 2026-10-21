package com.agnosticrouter.gmail.service;

import com.agnosticrouter.gmail.config.GmailSettings;
import com.agnosticrouter.gmail.repo.GmailRepository;
import com.agnosticrouter.gmail.gateway.GoogleMailClient;
import com.agnosticrouter.gmail.security.TokenCipher;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

/** OAuth consent, refresh token lifecycle and encrypted storage. */
@Service
public class GmailOAuthService {
    private final GmailRepository repo;
    private final GoogleMailClient google;
    private final TokenCipher tokens;
    private final GmailSettings cfg;
    public GmailOAuthService(GmailRepository repo, GoogleMailClient google, TokenCipher tokens, GmailSettings cfg) {
        this.repo=repo;this.google=google;this.tokens=tokens;this.cfg=cfg;
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
 public String access(GmailRepository.Account a){
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
