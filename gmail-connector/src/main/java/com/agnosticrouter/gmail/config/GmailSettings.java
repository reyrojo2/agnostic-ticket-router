package com.agnosticrouter.gmail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import com.agnosticrouter.gmail.application.port.GmailRuntimeSettings;

@ConfigurationProperties(prefix="app.gmail")
public record GmailSettings(String clientId,String clientSecret,String redirectUri,
                            String encryptionKey,int pollSeconds,String intakeUrl) implements GmailRuntimeSettings {
 public boolean configured(){return nonblank(clientId)&&nonblank(clientSecret)&&nonblank(encryptionKey);}
 private static boolean nonblank(String s){return s!=null&&!s.isBlank();}
}
