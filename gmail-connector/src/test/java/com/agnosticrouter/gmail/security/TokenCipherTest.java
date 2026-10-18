package com.agnosticrouter.gmail.security;

import com.agnosticrouter.gmail.config.GmailSettings;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenCipherTest {
 @Test void encryptsWithNonceAndRoundTrips(){
  String secret=Base64.getUrlEncoder().encodeToString(new byte[32]);
  TokenCipher cipher=new TokenCipher(new GmailSettings("client","secret","http://localhost/cb",secret,90,"http://intake"));
  String first=cipher.encrypt("refresh-token");
  String second=cipher.encrypt("refresh-token");
  assertNotEquals(first,second);
  assertEquals("refresh-token",cipher.decrypt(first));
  assertEquals("refresh-token",cipher.decrypt(second));
 }
}
