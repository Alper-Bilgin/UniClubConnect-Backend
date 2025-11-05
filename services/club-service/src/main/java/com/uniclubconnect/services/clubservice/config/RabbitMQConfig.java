package com.uniclubconnect.services.clubservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Bu değeri config-server'daki club-service.yml'den alacak
    @Value("${spring.rabbitmq.exchange.name}")
    private String exchangeName;

    // Yayın yapacağımız 'Exchange'i (dağıtıcı) tanımlar
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    // Gönderilecek nesnelerin (DTO'lar) JSON'a çevrilmesini sağlar
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}