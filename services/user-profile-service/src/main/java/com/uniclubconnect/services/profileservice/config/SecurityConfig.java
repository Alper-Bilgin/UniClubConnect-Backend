package com.uniclubconnect.services.profileservice.config;

import com.uniclubconnect.services.profileservice.security.AuthTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private AuthTokenFilter authTokenFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // 1. Dışarıdan veya diğer servislerden gelecek profil sorgulamalarına izin ver (Public)
                        .requestMatchers("/api/profiles/user/**").permitAll()
                        // 🔥 YENİ: Tüm kullanıcıları listeleme uç noktasını HERKESE AÇIK yap 🔥
                        .requestMatchers("/api/profiles/all").permitAll()

                        // 2. /api/profiles/me gibi geri kalan TÜM yollar kimlik doğrulaması gerektirir
                        .requestMatchers("/api/profiles/**").authenticated()

                        .anyRequest().authenticated()
                );

        // Kendi token filtremizi Spring Security zincirine ekle
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}