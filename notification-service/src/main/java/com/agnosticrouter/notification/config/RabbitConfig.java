package com.agnosticrouter.notification.config;
import com.agnosticrouter.contracts.RabbitNames;import org.springframework.amqp.core.*;import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;import org.springframework.context.annotation.*;
@Configuration public class RabbitConfig{
 @Bean TopicExchange e(){return new TopicExchange(RabbitNames.EXCHANGE,true,false);} @Bean Queue emailQ(){return QueueBuilder.durable(RabbitNames.Q_NOTIFICATION_EMAILS).build();}@Bean Queue sysQ(){return QueueBuilder.durable(RabbitNames.Q_NOTIFICATION_SYSTEM).build();}
 @Bean Binding b1(TopicExchange e,Queue emailQ){return BindingBuilder.bind(emailQ).to(e).with(RabbitNames.RK_EMAIL_RECEIVED);}@Bean Binding b2(TopicExchange e,Queue sysQ){return BindingBuilder.bind(sysQ).to(e).with(RabbitNames.RK_SYSTEM_EVENT);}@Bean Jackson2JsonMessageConverter c(){return new Jackson2JsonMessageConverter();}
}
