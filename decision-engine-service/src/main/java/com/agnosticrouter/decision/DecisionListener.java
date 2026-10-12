package com.agnosticrouter.decision;
import com.agnosticrouter.contracts.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
@Component
public class DecisionListener {
 private final GenericDecisionEngine engine; private final RabbitTemplate rabbit;
 public DecisionListener(GenericDecisionEngine e,RabbitTemplate r){engine=e;rabbit=r;}
 @RabbitListener(queues=RabbitNames.Q_DECISION_EMAILS)
 public void handle(IncomingEmailEvent email){ StandardTicketEvent out=engine.evaluate(email); rabbit.convertAndSend(RabbitNames.EXCHANGE,RabbitNames.RK_TICKET_STANDARDIZED,out); }
}
