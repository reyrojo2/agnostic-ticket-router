package com.agnosticrouter.gmail.service;

import com.agnosticrouter.gmail.repo.GmailRepository;
import com.agnosticrouter.gmail.gateway.GoogleMailClient;
import com.agnosticrouter.gmail.gateway.IntakePublisher;
import org.springframework.stereotype.Service;

/** Processes one Gmail message and records its stable identity after successful publishing. */
@Service
public class GmailMessageProcessor {
    private final GmailRepository repository;
    private final GoogleMailClient gmail;
    private final IntakePublisher intake;
    private final MailParser parser;

    public GmailMessageProcessor(GmailRepository repository,GoogleMailClient gmail,
                                 IntakePublisher intake,MailParser parser) {
        this.repository=repository;this.gmail=gmail;this.intake=intake;this.parser=parser;
    }

    public boolean process(GmailRepository.Account account,String accessToken,String messageId) {
        if(repository.processed(account.email(),messageId))return false;
        String raw=(String)gmail.message(accessToken,messageId).get("raw");
        if(raw==null)return false;
        MailParser.Mail email=parser.parse(raw);
        if(email.from()==null || !email.from().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
               ||email.subject().isBlank()||email.body().isBlank())return false;
        String requestId=intake.publish(account.profileCode(),email.from(),
                email.subject(),email.body(),messageId);
        repository.markProcessed(account.email(),messageId,requestId);
        return true;
    }
}
