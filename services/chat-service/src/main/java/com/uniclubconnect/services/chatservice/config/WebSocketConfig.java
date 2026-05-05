package com.uniclubconnect.services.chatservice.config;

import com.uniclubconnect.services.chatservice.security.AuthChannelInterceptor;
import com.uniclubconnect.services.chatservice.security.HttpHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthChannelInterceptor authChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Sunucudan istemciye giden mesaj kanalları
        config.enableSimpleBroker("/topic", "/queue");
        // İstemciden sunucuya gelen mesajların prefix'i (@MessageMapping için)
        config.setApplicationDestinationPrefixes("/app");
        // Kullanıcıya özel mesajlar için prefix (/user/queue/messages gibi)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        // 1. SAF WEBSOCKET (Postman, iOS, Android için)
        // URL: ws://localhost:9012/ws/chat
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HttpHandshakeInterceptor());

        // 2. SOCKJS (Web Frontend, React, Vue vs. için)
        // URL: ws://localhost:9012/ws/chat/sockjs
        registry.addEndpoint("/ws/chat/sockjs")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HttpHandshakeInterceptor())
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Güvenlik interceptor'ını sisteme dahil et
        registration.interceptors(authChannelInterceptor);
    }
}
