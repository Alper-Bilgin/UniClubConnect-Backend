package com.uniclubconnect.services.postservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class RabbitMQConfig {

    @Value("${post.rabbitmq.exchange}")
    private String exchange;

    // Post Service mesaj okumayacak (Listener yok), SADECE mesaj gönderecek.
    // Bu yüzden Queue (Kuyruk) veya Binding tanımlamamıza gerek yok.
    // Sadece mesajı atacağı "Santrali" (Exchange) oluşturması yeterli.

    @Bean
    public TopicExchange postExchange() {
        return new TopicExchange(exchange);
    }

    // Tarih dönüştürme hatalarını (LocalDateTime) önlemek için Jackson ayarı
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
