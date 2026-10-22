package com.agnosticrouter.gmail.application.port;

import java.util.Map;
public interface IntakePublisher {
 String publish(String profile,String from,String subject,String body,String messageId);
}
