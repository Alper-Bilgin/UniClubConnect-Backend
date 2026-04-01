package com.uniclubconnect.services.gamificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${gamification.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${gamification.rabbitmq.queue}")
    private String queueName;

    @Value("${gamification.rabbitmq.routing-key}")
    private String routingKey;

    @Bean
    public TopicExchange gamificationExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue gamificationQueue() {
        return new Queue(queueName, true); // Kalıcı (Durable) kuyruk
    }

    @Bean
    public Binding bindingGamificationQueue() {
        return BindingBuilder.bind(gamificationQueue()).to(gamificationExchange()).with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}
