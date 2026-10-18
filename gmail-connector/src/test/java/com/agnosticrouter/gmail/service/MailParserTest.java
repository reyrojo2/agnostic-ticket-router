package com.agnosticrouter.gmail.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MailParserTest {
 @Test void extractsPlaintextSenderAndSubject()throws Exception{
  MimeMessage email=new MimeMessage(Session.getInstance(new Properties()));
  email.setFrom("operations@example.test");
  email.setSubject("Cambio de RUC","UTF-8");
  email.setText("Solicito cambio de titularidad","UTF-8");
  email.saveChanges();
  ByteArrayOutputStream output=new ByteArrayOutputStream();
  email.writeTo(output);
  String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(output.toByteArray());
  MailParser.Mail parsed=new MailParser().parse(raw);
  assertEquals("operations@example.test",parsed.from());
  assertEquals("Cambio de RUC",parsed.subject());
  assertEquals("Solicito cambio de titularidad",parsed.body());
 }
 @Test void rejectsMalformedMessage(){
  assertThrows(IllegalArgumentException.class,()->new MailParser().parse("??invalid??"));
 }
}
