package com.agnosticrouter.decision;

import com.agnosticrouter.contracts.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Idempotent decision consumer. Failed messages are quarantined; never hot-loop on a paid API. */
@Component
public class DecisionListener {
 private final GenericDecisionEngine engine;
 private final RabbitTemplate rabbit;
 private final JdbcTemplate jdbc;

 public DecisionListener(GenericDecisionEngine e, RabbitTemplate r, JdbcTemplate jdbc) {
    engine=e; rabbit=r; this.jdbc=jdbc;
 }
 @RabbitListener(queues=RabbitNames.Q_DECISION_EMAILS, concurrency="1")
 public void handle(IncomingEmailEvent email) {
    try {
       if(email.requestId()==null || email.requestId().isBlank()) throw new IllegalArgumentException("Missing requestId");
       Integer seen=jdbc.queryForObject("SELECT count(*) FROM decision_engine.processed_request WHERE request_id=?",
           Integer.class,email.requestId());
       if(seen!=null && seen>0) return;
       StandardTicketEvent out=engine.evaluate(email);
       rabbit.convertAndSend(RabbitNames.EXCHANGE,RabbitNames.RK_TICKET_STANDARDIZED,out);
       jdbc.update("INSERT INTO decision_engine.processed_request(request_id) VALUES (?) ON CONFLICT DO NOTHING",email.requestId());
    } catch (RuntimeException e) {
       // Failed messages remain inspectable; return normally to acknowledge the original delivery.
       rabbit.convertAndSend(RabbitNames.EXCHANGE,"decision.email.failed",email);
    }
 }
}
