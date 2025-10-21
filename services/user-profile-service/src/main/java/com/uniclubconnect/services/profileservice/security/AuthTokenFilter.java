package com.uniclubconnect.services.profileservice.security;

import com.uniclubconnect.services.profileservice.security.jwt.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

                // Bu servis User veritabanına sahip değil.
                // Kullanıcı detaylarını (rol, id) doğrudan token'ın 'claims' kısmından okuruz.
                Claims claims = jwtUtils.getAllClaimsFromToken(jwt);
                String email = claims.getSubject();
                String authId = claims.get("userId", String.class);

                List<Map<String, String>> rolesMap = claims.get("roles", List.class);
                List<GrantedAuthority> authorities = rolesMap.stream()
                        .map(roleMap -> new SimpleGrantedAuthority(roleMap.get("authority")))
                        .collect(Collectors.toList());

                // Spring Security için Authentication nesnesini oluştur
                // Not: Principal olarak 'authId'yi saklamak, Controller'da işimizi kolaylaştırır.
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        authId, // Principal (kimlik)
                        null,
                        authorities);

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Kullanıcıyı SecurityContext'e ata
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Kullanıcı kimliği doğrulanamadı: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7); // "Bearer " kısmını at
        }
        return null;
    }
}