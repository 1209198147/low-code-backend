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

    @Bean("deleteAppQueue")
    public Queue deleteAppQueue(){
        return new Queue("deleteAppQueue", true);
    }

    @Bean("exchange")
    public DirectExchange screenShotExchange() {
        return new DirectExchange("exchange");
    }

    @Bean("screenShotBinding")
    public Binding screenShotBinding(@Qualifier("screenShotQueue") Queue screenShotQueue,
                           @Qualifier("exchange") DirectExchange exchange) {
        return BindingBuilder.bind(screenShotQueue)
                .to(exchange)
                .with("screenShot");
    }

    @Bean("deleteAppBinding")
    public Binding deleteAppBinding(@Qualifier("deleteAppQueue") Queue deleteAppQueue,
                           @Qualifier("exchange") DirectExchange exchange) {
        return BindingBuilder.bind(deleteAppQueue)
                .to(exchange)
                .with("deleteApp");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
