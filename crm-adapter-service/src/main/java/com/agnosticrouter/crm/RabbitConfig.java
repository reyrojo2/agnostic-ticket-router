package com.agnosticrouter.crm;
import com.agnosticrouter.contracts.RabbitNames;import org.springframework.amqp.core.*;import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;import org.springframework.context.annotation.*;
@Configuration public class RabbitConfig{
 @Bean TopicExchange e(){return new TopicExchange(RabbitNames.EXCHANGE,true,false);} @Bean Queue q(){return QueueBuilder.durable(RabbitNames.Q_CRM_STANDARD_TICKETS).build();}
 @Bean Queue sys(){return QueueBuilder.durable(RabbitNames.Q_NOTIFICATION_SYSTEM).build();}
 @Bean Binding b(TopicExchange e,Queue q){return BindingBuilder.bind(q).to(e).with(RabbitNames.RK_TICKET_STANDARDIZED);} @Bean Binding bs(TopicExchange e,Queue sys){return BindingBuilder.bind(sys).to(e).with(RabbitNames.RK_SYSTEM_EVENT);} @Bean Jackson2JsonMessageConverter c(){return new Jackson2JsonMessageConverter();}
}
