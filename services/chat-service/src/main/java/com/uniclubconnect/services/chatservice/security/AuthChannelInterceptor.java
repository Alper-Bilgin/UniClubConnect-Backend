package com.uniclubconnect.services.chatservice.security;

import com.uniclubconnect.services.chatservice.security.dto.UserPrincipal;
import com.uniclubconnect.services.chatservice.security.jwt.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            String jwt = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
            }
            // Direktörün uyarısı: SockJS Session'dan (Handshake) token'ı al
            else if (accessor.getSessionAttributes() != null && accessor.getSessionAttributes().containsKey("token")) {
                jwt = (String) accessor.getSessionAttributes().get("token");
            }
            else if (accessor.getFirstNativeHeader("token") != null) {
                jwt = accessor.getFirstNativeHeader("token");
            }

            // 👇 KRİTİK: Token yoksa veya geçersizse bağlantıyı KES 👇
            if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
                throw new IllegalArgumentException("Invalid or missing JWT token for WebSocket connection");
            }

            Claims claims = jwtUtils.getAllClaimsFromToken(jwt);
            String authId = claims.get("userId", String.class);
            String email = claims.getSubject();

            List<GrantedAuthority> authorities = ((List<Map<String, String>>) claims.get("roles")).stream()
                    .map(roleMap -> new SimpleGrantedAuthority(roleMap.get("authority")))
                    .collect(Collectors.toList());

            UserPrincipal principal = new UserPrincipal(
                    authId,
                    email,
                    authorities.stream().map(GrantedAuthority::getAuthority).toList()
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            accessor.setUser(authentication);
        }
        return message;
    }
}