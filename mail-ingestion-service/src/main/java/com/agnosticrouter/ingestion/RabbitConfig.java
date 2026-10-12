package com.agnosticrouter.ingestion;

import com.agnosticrouter.contracts.RabbitNames;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean TopicExchange ticketExchange(){ return new TopicExchange(RabbitNames.EXCHANGE, true, false); }
    @Bean Queue decisionQueue(){ return QueueBuilder.durable(RabbitNames.Q_DECISION_EMAILS).build(); }
    @Bean Queue notificationEmailQueue(){ return QueueBuilder.durable(RabbitNames.Q_NOTIFICATION_EMAILS).build(); }
    @Bean Binding decisionBinding(TopicExchange ticketExchange, Queue decisionQueue){ return BindingBuilder.bind(decisionQueue).to(ticketExchange).with(RabbitNames.RK_EMAIL_RECEIVED); }
    @Bean Binding notificationEmailBinding(TopicExchange ticketExchange, Queue notificationEmailQueue){ return BindingBuilder.bind(notificationEmailQueue).to(ticketExchange).with(RabbitNames.RK_EMAIL_RECEIVED); }
    @Bean Jackson2JsonMessageConverter jsonConverter(){ return new Jackson2JsonMessageConverter(); }
}
