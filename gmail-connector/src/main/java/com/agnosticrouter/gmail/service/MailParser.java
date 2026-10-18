package com.agnosticrouter.gmail.service;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import org.springframework.stereotype.Component;

@Component
public class MailParser {
 public record Mail(String from,String subject,String body){}
 public Mail parse(String raw){
  try{
   byte[] bytes=Base64.getUrlDecoder().decode(raw);
   MimeMessage msg=new MimeMessage(Session.getInstance(new Properties()),new ByteArrayInputStream(bytes));
   Address[] senders=msg.getFrom();
   String from=(senders==null||senders.length==0)?"":((jakarta.mail.internet.InternetAddress)senders[0]).getAddress();
   String subject=msg.getSubject()==null?"":msg.getSubject();
   String body=extract(msg);
   if(subject.length()>500)subject=subject.substring(0,500);
   if(body.length()>50000)body=body.substring(0,50000);
   return new Mail(from,subject,body);
  }catch(Exception e){throw new IllegalArgumentException("Mensaje MIME inválido",e);}
 }
 private String extract(Part p)throws Exception{
  if(p.isMimeType("text/plain")){
   Object content=p.getContent();return content instanceof String s?s:"";
  }
  if(p.isMimeType("multipart/*")){
   Multipart m=(Multipart)p.getContent();
   for(int i=0;i<m.getCount();i++){
    BodyPart child=m.getBodyPart(i);
    if(child.getDisposition()!=null&&Part.ATTACHMENT.equalsIgnoreCase(child.getDisposition()))continue;
    String text=extract(child);if(!text.isBlank())return text;
   }
  }
  return "";
 }
}
