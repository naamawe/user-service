package com.xhx.userservice.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author master
 */
@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "log.direct.exchange";
    public static final String QUEUE = "log.user.queue";
    public static final String ROUTING_KEY = "log.user.register";

    @Bean
    public DirectExchange logExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue logQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding logBinding() {
        return BindingBuilder
                .bind(logQueue())
                .to(logExchange())
                .with(ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter());
        return template;
    }
}
