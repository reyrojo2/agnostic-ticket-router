package com.agnosticrouter.decision;
import com.agnosticrouter.contracts.RabbitNames;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.*;
@Configuration
public class RabbitConfig {
 @Bean TopicExchange ticketExchange(){return new TopicExchange(RabbitNames.EXCHANGE,true,false);}
 @Bean Queue decisionQueue(){return QueueBuilder.durable(RabbitNames.Q_DECISION_EMAILS)
   .deadLetterExchange(RabbitNames.EXCHANGE).deadLetterRoutingKey("decision.email.failed").build();}
 @Bean Queue failedDecisionQueue(){return QueueBuilder.durable("decision.email.failed.q").build();}
 @Bean Binding failedDecisionBinding(TopicExchange e,Queue failedDecisionQueue){return BindingBuilder.bind(failedDecisionQueue).to(e).with("decision.email.failed");}
 @Bean Queue crmQueue(){return QueueBuilder.durable(RabbitNames.Q_CRM_STANDARD_TICKETS).build();}
 @Bean Binding decisionBinding(TopicExchange e,Queue decisionQueue){return BindingBuilder.bind(decisionQueue).to(e).with(RabbitNames.RK_EMAIL_RECEIVED);}
 @Bean Binding crmBinding(TopicExchange e,Queue crmQueue){return BindingBuilder.bind(crmQueue).to(e).with(RabbitNames.RK_TICKET_STANDARDIZED);}
 @Bean Jackson2JsonMessageConverter converter(){return new Jackson2JsonMessageConverter();}
}
