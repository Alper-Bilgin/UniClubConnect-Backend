package com.uniclubconnect.services.chatservice.config;

import com.uniclubconnect.services.chatservice.security.dto.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final StringRedisTemplate redisTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken auth) {
            UserPrincipal user = (UserPrincipal) auth.getPrincipal();
            String userId = user.getAuthId();

            // 👇 KRİTİK DÜZELTME: TTL 60 Saniye! (Frontend 30 sn'de bir ping atacak)
            redisTemplate.opsForValue().set("online:user:" + userId, "true", Duration.ofSeconds(60));
            log.info("🟢 Kullanıcı bağlandı: {}", userId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken auth) {
            UserPrincipal user = (UserPrincipal) auth.getPrincipal();
            String userId = user.getAuthId();

            // Bağlantı koptuğunda Redis'ten sil
            redisTemplate.delete("online:user:" + userId);
            log.info("🔴 Kullanıcı ayrıldı: {}", userId);
        }
    }
}
