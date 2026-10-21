package com.agnosticrouter.decision.gateway;

import com.agnosticrouter.decision.service.*;
import com.agnosticrouter.decision.*;

import com.agnosticrouter.contracts.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Idempotent decision consumer. Failed messages are quarantined; never hot-loop on a paid API. */
@Component
public class DecisionListener {
 private static final Logger log=LoggerFactory.getLogger(DecisionListener.class);
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
       Integer seen=jdbc.queryForObject("""
          SELECT count(*) FROM (
            SELECT request_id FROM decision_engine.processed_request WHERE request_id=?
            UNION
            SELECT request_id FROM observability.request_trace
            WHERE request_id=? AND classified_at IS NOT NULL
          ) completed
          """,
           Integer.class,email.requestId(),email.requestId());
       if(seen!=null && seen>0) {
          log.info("Skipping already classified requestId={}", email.requestId());
          return;
       }
       StandardTicketEvent out=engine.evaluate(email);
       rabbit.convertAndSend(RabbitNames.EXCHANGE,RabbitNames.RK_TICKET_STANDARDIZED,out);
       jdbc.update("INSERT INTO decision_engine.processed_request(request_id) VALUES (?) ON CONFLICT DO NOTHING",email.requestId());
    } catch (RuntimeException e) {
       log.error("Decision processing failed; quarantining requestId={} to decision.email.failed.q", email.requestId(), e);
       // Failed messages remain inspectable; return normally to acknowledge the original delivery.
       rabbit.convertAndSend(RabbitNames.EXCHANGE,"decision.email.failed",email);
    }
 }
}
