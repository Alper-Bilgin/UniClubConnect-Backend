package com.uniclubconnect.services.notificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

    // 1. Hoşgeldin Maili Ayarları
    @Value("${notification.rabbitmq.queue.welcome-email}")
    private String welcomeQueueName;

    @Value("${notification.rabbitmq.exchange.user}")
    private String userExchangeName;

    @Value("${notification.rabbitmq.routing-key.user-created}")
    private String userCreatedRoutingKey;

    // 2. Bilet Maili Ayarları
    @Value("${notification.rabbitmq.queue.ticket-email}")
    private String ticketQueueName;

    @Value("${notification.rabbitmq.exchange.notification}")
    private String notificationExchangeName;

    @Value("${notification.rabbitmq.routing-key.ticket-created}")
    private String ticketCreatedRoutingKey;

    // Kuyruklar
    @Bean
    public Queue welcomeQueue() { return new Queue(welcomeQueueName); }

    @Bean
    public Queue ticketQueue() { return new Queue(ticketQueueName); }

    // Exchange'ler (Zaten varlar ama bağlamak için tanımlıyoruz)
    @Bean
    public TopicExchange userExchange() { return new TopicExchange(userExchangeName); }

    @Bean
    public TopicExchange notificationExchange() { return new TopicExchange(notificationExchangeName); }

    // Bağlantılar (Bindings)
    @Bean
    public Binding welcomeBinding() {
        return BindingBuilder.bind(welcomeQueue()).to(userExchange()).with(userCreatedRoutingKey);
    }

    @Bean
    public Binding ticketBinding() {
        return BindingBuilder.bind(ticketQueue()).to(notificationExchange()).with(ticketCreatedRoutingKey);
    }

    // 3. Follow Maili Ayarları
    @Value("${notification.rabbitmq.queue.follow-email}")
    private String followQueueName;

    @Value("${notification.rabbitmq.exchange.follow}")
    private String followExchangeName;

    @Value("${notification.rabbitmq.routing-key.follow-event}")
    private String followRoutingKey;

    // YENİ BEAN'LER
    @Bean
    public Queue followQueue() { return new Queue(followQueueName); }

    @Bean
    public TopicExchange followExchange() { return new TopicExchange(followExchangeName); }

    @Bean
    public Binding followBinding() {
        return BindingBuilder.bind(followQueue()).to(followExchange()).with(followRoutingKey);
    }


    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Tarih hatasını çözen altın vuruş!
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    // 4. Chat Bildirimi Ayarları
    @Value("${notification.rabbitmq.queue.chat-notification}")
    private String chatQueueName;

    @Value("${notification.rabbitmq.exchange.chat}")
    private String chatExchangeName;

    @Value("${notification.rabbitmq.routing-key.unread-message}")
    private String chatRoutingKey;

    @Bean
    public Queue chatQueue() { return new Queue(chatQueueName); }

    @Bean
    public TopicExchange chatExchange() { return new TopicExchange(chatExchangeName); }

    @Bean
    public Binding chatBinding() {
        return BindingBuilder.bind(chatQueue()).to(chatExchange()).with(chatRoutingKey);
    }
}