package com.agnosticrouter.gmail.security;

import com.agnosticrouter.gmail.config.GmailSettings;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class TokenCipher {
 private final SecretKeySpec key;
 private final SecureRandom random=new SecureRandom();
 public TokenCipher(GmailSettings settings){
  // Separate AES-GCM key from the older Fernet format; tokens must be reauthorized.
  String env=settings.encryptionKey();
  if(env==null||env.isBlank()){key=null;return;}
  byte[] raw=Base64.getUrlDecoder().decode(env);
  if(raw.length<32)throw new IllegalArgumentException("GMAIL_ENCRYPTION_KEY debe contener al menos 32 bytes");
  key=new SecretKeySpec(java.util.Arrays.copyOf(raw,32),"AES");
 }
 public String encrypt(String value){
  if(key==null)throw new IllegalStateException("GMAIL_ENCRYPTION_KEY no configurada");
  try{
   byte[] nonce=new byte[12];random.nextBytes(nonce);
   Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
   c.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,nonce));
   byte[] ct=c.doFinal(value.getBytes(StandardCharsets.UTF_8));
   byte[] data=new byte[nonce.length+ct.length];
   System.arraycopy(nonce,0,data,0,nonce.length);System.arraycopy(ct,0,data,nonce.length,ct.length);
   return "v2:"+Base64.getUrlEncoder().withoutPadding().encodeToString(data);
  }catch(Exception e){throw new IllegalStateException("Token encryption failed",e);}
 }
 public String decrypt(String encrypted){
  if(key==null||encrypted==null||!encrypted.startsWith("v2:"))throw new IllegalStateException("Token encryption unavailable");
  try{
   byte[] data=Base64.getUrlDecoder().decode(encrypted.substring(3));
   byte[] nonce=java.util.Arrays.copyOfRange(data,0,12);
   Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,nonce));
   return new String(c.doFinal(data,12,data.length-12),StandardCharsets.UTF_8);
  }catch(Exception e){throw new IllegalStateException("Token decryption failed",e);}
 }
}
