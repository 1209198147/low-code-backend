package com.shikou.aicode.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    @Bean("screenShotQueue")
    public Queue screenShotQueue(){
        return new Queue("screenShotQueue", true);
    }

    @Bean("screenShotExchange")
    public DirectExchange screenShotExchange() {
        return new DirectExchange("screenShotExchange");
    }

    @Bean
    public Binding binding(@Qualifier("screenShotQueue") Queue screenShotQueue,
                           @Qualifier("screenShotExchange") DirectExchange screenShotExchange) {
        return BindingBuilder.bind(screenShotQueue)
                .to(screenShotExchange)
                .with("screenShot");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
