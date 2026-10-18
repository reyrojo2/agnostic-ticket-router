package com.agnosticrouter.observability.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    Declarables telemetryBindings() {
        TopicExchange exchange = new TopicExchange("ticket.events",true,false);
        Queue email = QueueBuilder.durable("observability.email.q").build();
        Queue ticket = QueueBuilder.durable("observability.ticket.q").build();
        Queue system = QueueBuilder.durable("observability.system.q").build();
        return new Declarables(exchange,email,ticket,system,
                BindingBuilder.bind(email).to(exchange).with("email.received"),
                BindingBuilder.bind(ticket).to(exchange).with("ticket.standardized"),
                BindingBuilder.bind(system).to(exchange).with("system.event"));
    }
    @Bean
    Jackson2JsonMessageConverter jacksonConverter(){return new Jackson2JsonMessageConverter();}
}
