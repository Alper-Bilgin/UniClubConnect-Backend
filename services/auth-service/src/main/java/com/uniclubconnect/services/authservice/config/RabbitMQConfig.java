package com.uniclubconnect.services.authservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Mevcut Auth Exchange
    @Value("${auth.rabbitmq.exchange}")
    private String authExchangeName;

    // YENİ: Gamification Exchange
    @Value("${gamification.rabbitmq.exchange}")
    private String gamificationExchangeName;

    // Mevcut Bean'in adını authExchange yaptık ki Spring'in kafası karışmasın
    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(authExchangeName);
    }

    // YENİ: Gamification Exchange Bean'i
    @Bean
    public TopicExchange gamificationExchange() {
        return new TopicExchange(gamificationExchangeName);
    }

    // Mesajları JSON formatında göndermek için dönüştürücü
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}