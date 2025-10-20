package com.uniclubconnect.services.authservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Bu değerler config-server'daki auth-service.yml'den gelecek
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routingkey.user_created}")
    private String userCreatedRoutingKey;

    // Diğer servislerin (örn: profile-service) dinlemesi için
    // bir kuyruk (Queue) da burada tanımlayalım.
    // İleride profile-service'i yazarken aynı kuyruk adını kullanacağız.
    private String userCreatedQueueName = "profile_user_created_queue";


    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Queue userCreatedQueue() {
        return new Queue(userCreatedQueueName);
    }

    // Exchange'i, routing key aracılığıyla kuyruğa bağla
    @Bean
    public Binding binding() {
        return BindingBuilder
                .bind(userCreatedQueue())
                .to(exchange())
                .with(userCreatedRoutingKey);
    }

    // Not: Spring Boot, RabbitTemplate'i otomatik olarak yapılandırır,
    // bizim sadece Exchange/Queue tanımlamamız yeterlidir.

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}