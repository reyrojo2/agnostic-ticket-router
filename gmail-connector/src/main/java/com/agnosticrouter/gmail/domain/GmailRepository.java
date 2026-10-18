package com.agnosticrouter.gmail.domain;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class GmailRepository {
 public record Account(String email,String refreshToken,String accessToken,long expiresAt,String profileCode,
                       String query,boolean enabled,Instant lastSync,String lastError){}
 private final JdbcTemplate jdbc;
 public GmailRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public Account account(){
  List<Account> rows=jdbc.query("SELECT * FROM gmail_connector.account WHERE id=1",
      (r,n)->new Account(r.getString("email"),r.getString("refresh_token"),
      r.getString("access_token"),r.getLong("expires_at"),r.getString("profile_code"),
      r.getString("query_text"),r.getBoolean("enabled"),
      r.getTimestamp("last_sync")==null?null:r.getTimestamp("last_sync").toInstant(),
      r.getString("last_error")));
  return rows.isEmpty()?null:rows.get(0);
 }
 public void saveAccount(String email,String refresh,String access,long expiry){
  jdbc.update("""
    INSERT INTO gmail_connector.account(id,email,refresh_token,access_token,expires_at,enabled)
    VALUES(1,?,?,?,?,false)
    ON CONFLICT(id) DO UPDATE SET email=EXCLUDED.email,refresh_token=EXCLUDED.refresh_token,
     access_token=EXCLUDED.access_token,expires_at=EXCLUDED.expires_at,
     enabled=false,last_error=null
    """,email,refresh,access,expiry);
 }
 public void access(String token,long expiry){jdbc.update("UPDATE gmail_connector.account SET access_token=?,expires_at=? WHERE id=1",token,expiry);}
 public void settings(String profile,String query,boolean enabled){jdbc.update("UPDATE gmail_connector.account SET profile_code=?,query_text=?,enabled=? WHERE id=1",profile,query,enabled);}
 public void synced(){jdbc.update("UPDATE gmail_connector.account SET last_sync=now(),last_error=null WHERE id=1");}
 public void failed(String error){jdbc.update("UPDATE gmail_connector.account SET last_error=? WHERE id=1",error);}
 public void disconnect(){jdbc.update("DELETE FROM gmail_connector.account WHERE id=1");jdbc.update("DELETE FROM gmail_connector.oauth_state");}
 public void state(String state){
  jdbc.update("DELETE FROM gmail_connector.oauth_state WHERE expires_at<?",Instant.now().getEpochSecond());
  jdbc.update("INSERT INTO gmail_connector.oauth_state(token,expires_at) VALUES (?,?)",state,Instant.now().getEpochSecond()+600);
 }
 public boolean consumeState(String state){
  int updated=jdbc.update("DELETE FROM gmail_connector.oauth_state WHERE token=? AND expires_at>=?",state,Instant.now().getEpochSecond());
  return updated==1;
 }
 public boolean processed(String email,String id){
  Integer n=jdbc.queryForObject("SELECT count(*) FROM gmail_connector.processed WHERE email=? AND message_id=?",Integer.class,email,id);
  return n!=null&&n>0;
 }
 public void markProcessed(String email,String id,String requestId){
  jdbc.update("INSERT INTO gmail_connector.processed(email,message_id,request_id,processed_at) VALUES(?,?,?,?) ON CONFLICT DO NOTHING",
      email,id,requestId,Instant.now().getEpochSecond());
 }
}
