package com.uniclubconnect.services.profileservice.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Bu bean, RabbitMQ'dan gelen JSON payload'ları
    // otomatik olarak DTO'muza (UserCreatedEvent) dönüştürür.
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Not: Kuyruk ve Exchange 'auth-service' tarafında zaten tanımlandı (idempotent).
    // Burada tekrar tanımlamamıza gerek yok, sadece dinleyeceğiz.
}