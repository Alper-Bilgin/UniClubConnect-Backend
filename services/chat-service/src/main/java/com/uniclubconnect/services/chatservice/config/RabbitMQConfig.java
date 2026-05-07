package com.uniclubconnect.services.chatservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Notification servisinde tanımladığımız isimlerle BİREBİR aynı olmalı
    public static final String CHAT_EXCHANGE = "chat_exchange";
    public static final String UNREAD_MESSAGE_ROUTING_KEY = "chat.message.unread";

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        // LocalDateTime objelerini JSON'a çevirirken hata vermemesi için kritik modül
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
