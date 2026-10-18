package com.agnosticrouter.gmail.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GmailSettings.class)
public class ClientConfig {
 @Bean RestClient gmailHttp(){return RestClient.builder().build();}
}
