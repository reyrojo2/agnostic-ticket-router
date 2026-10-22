package com.agnosticrouter.notification.infrastructure.sender;

import com.agnosticrouter.notification.domain.model.NotificationChannel;
import com.agnosticrouter.notification.application.port.NotificationSender;
import com.agnosticrouter.notification.application.service.SecretResolver;
import java.util.Properties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
public class SmtpNotificationSender implements NotificationSender {
    private final SecretResolver secrets;
    public SmtpNotificationSender(SecretResolver secrets) { this.secrets = secrets; }
    @Override public String type() { return "SMTP"; }

    @Override
    public void send(NotificationChannel channel, String recipient, String subject, String body) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(channel.getHost());
        sender.setPort(channel.getPort());
        sender.setUsername(secrets.resolve(channel.getUsernameRef()));
        sender.setPassword(secrets.resolve(channel.getPasswordRef()));
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", Boolean.toString(Boolean.TRUE.equals(channel.getSmtpAuth())));
        properties.put("mail.smtp.starttls.enable", Boolean.toString(Boolean.TRUE.equals(channel.getStartTls())));
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(channel.getFromAddress());
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
    }
}
