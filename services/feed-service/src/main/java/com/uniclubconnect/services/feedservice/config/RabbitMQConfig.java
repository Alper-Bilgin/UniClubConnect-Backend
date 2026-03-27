package com.uniclubconnect.services.feedservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class RabbitMQConfig {

    @Value("${feed.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${feed.rabbitmq.queue.post-event-queue}")
    private String postEventQueueName;

    @Value("${feed.rabbitmq.routing-key.post-created}")
    private String postCreatedRoutingKey;

    @Value("${feed.rabbitmq.routing-key.post-deleted}")
    private String postDeletedRoutingKey;

    @Bean
    public Queue postEventQueue() {
        return new Queue(postEventQueueName);
    }

    @Bean
    public TopicExchange postExchange() {
        return new TopicExchange(exchangeName);
    }

    // Hem CREATE hem DELETE eventlerini aynı kuyruğa bağlamak için iki binding yapıyoruz
    @Bean
    public Binding bindingPostCreated() {
        return BindingBuilder.bind(postEventQueue())
                .to(postExchange())
                .with(postCreatedRoutingKey);
    }

    @Bean
    public Binding bindingPostDeleted() {
        return BindingBuilder.bind(postEventQueue())
                .to(postExchange())
                .with(postDeletedRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
