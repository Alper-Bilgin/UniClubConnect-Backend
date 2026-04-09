package com.uniclubconnect.services.registrationservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class RabbitMQConfig {

    // MEVCUT: Email/Bildirim Exchange
    @Value("${spring.rabbitmq.exchange.name}")
    private String exchangeName;

    // YENİ: Gamification Exchange
    @Value("${gamification.rabbitmq.exchange:gamification.exchange}")
    private String gamificationExchangeName;

    // MEVCUT BEAN
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    // YENİ BEAN
    @Bean
    public TopicExchange gamificationExchange() {
        return new TopicExchange(gamificationExchangeName, true, false);
    }

    // JSON Converter
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // RabbitTemplate (Global olarak JSON kullanması için)
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
