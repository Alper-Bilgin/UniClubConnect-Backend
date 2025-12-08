package com.uniclubconnect.services.authservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // YAML dosyasındaki "auth.rabbitmq.exchange" değerini okur
    @Value("${auth.rabbitmq.exchange}")
    private String exchangeName;

    // Sadece Exchange (Dağıtıcı) tanımlıyoruz.
    // Kuyrukları (Queue) dinleyen servisler (Notification, Profile) kendileri oluşturacak.
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    // Mesajları JSON formatında göndermek için dönüştürücü
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}