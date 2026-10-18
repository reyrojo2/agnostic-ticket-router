package com.agnosticrouter.gmail.gateway;

import com.agnosticrouter.gmail.config.GmailSettings;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GoogleMailRestClient implements GoogleMailClient {
 private final RestClient http;private final GmailSettings cfg;
 public GoogleMailRestClient(RestClient gmailHttp,GmailSettings cfg){this.http=gmailHttp;this.cfg=cfg;}
 @Override public Map<String,Object> exchangeCode(String code){
  return postForm(Map.of("code",code,"client_id",cfg.clientId(),"client_secret",cfg.clientSecret(),
     "redirect_uri",cfg.redirectUri(),"grant_type","authorization_code"));
 }
 @Override public Map<String,Object> refresh(String refreshToken){
  return postForm(Map.of("client_id",cfg.clientId(),"client_secret",cfg.clientSecret(),
      "refresh_token",refreshToken,"grant_type","refresh_token"));
 }
 @SuppressWarnings("unchecked")
 private Map<String,Object> postForm(Map<String,String> args){
  org.springframework.util.LinkedMultiValueMap<String,String> form=new org.springframework.util.LinkedMultiValueMap<>();
  args.forEach(form::add);
  return http.post().uri("https://oauth2.googleapis.com/token")
    .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
    .body(form).retrieve().body(Map.class);
 }
 @Override public Map<String,Object> profile(String token){
  return http.get().uri("https://gmail.googleapis.com/gmail/v1/users/me/profile")
      .headers(h->h.setBearerAuth(token)).retrieve().body(Map.class);
 }
 @Override public Map<String,Object> list(String token,String query,String page){
  var uri=UriComponentsBuilder.fromUriString("https://gmail.googleapis.com/gmail/v1/users/me/messages")
      .queryParam("q",query).queryParam("maxResults",50);
  if(page!=null&&!page.isBlank())uri.queryParam("pageToken",page);
  return http.get().uri(uri.build().encode().toUri()).headers(h->h.setBearerAuth(token))
      .retrieve().body(Map.class);
 }
 @Override public Map<String,Object> message(String token,String id){
  return http.get().uri("https://gmail.googleapis.com/gmail/v1/users/me/messages/{id}?format=raw",id)
      .headers(h->h.setBearerAuth(token)).retrieve().body(Map.class);
 }
}
