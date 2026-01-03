package com.uniclubconnect.services.profileservice.config;

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

    // 1. YAML dosyasından değerleri çekiyoruz
    @Value("${profile.rabbitmq.queue}")
    private String queueName;

    @Value("${auth.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${auth.rabbitmq.routing-key}")
    private String routingKey;

    // 2. Kuyruğu (Queue) Oluşturuyoruz
    // Bu, mesajların birikeceği kutudur.
    @Bean
    public Queue queue() {
        return new Queue(queueName, true); // true = RabbitMQ kapanıp açılsa da kuyruk silinmez (Durable)
    }

    // 3. Exchange'i Tanımlıyoruz
    // Auth servisinin mesajı attığı yer.
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    // 4. BAĞLANTIYI (BINDING) KURUYORUZ (En Önemli Kısım)
    // "Bu Exchange'e, şu Routing Key ile gelen mesajı -> Benim Queue'me yönlendir" diyoruz.
    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(routingKey);
    }

    // 5. JSON Dönüştürücü
    // Mesajlar byte[] olarak değil, JSON olarak işlensin diye.
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}