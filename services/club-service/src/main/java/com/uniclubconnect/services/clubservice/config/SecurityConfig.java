package com.uniclubconnect.services.clubservice.config;

import com.uniclubconnect.services.clubservice.security.AuthTokenFilter;
import com.uniclubconnect.services.clubservice.security.jwt.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // <-- Gerekli Import
import org.springframework.security.config.http.SessionCreationPolicy; // <-- Gerekli Import
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration; // <-- Gerekli Import
import org.springframework.web.cors.CorsConfigurationSource; // <-- Gerekli Import
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // <-- Gerekli Import

import java.util.Arrays; // <-- Gerekli Import

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtUtils jwtUtils; // Sadece JwtUtils'i enjekte et

    // AuthTokenFilter'ı manuel olarak oluştur
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter(jwtUtils); // JwtUtils'i kurucu metoda ver
    }

    // CORS (Cross-Origin) ayarları - 403 hatasını önler
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*")); // Herkese izin ver
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); // Tüm metotlara
        configuration.setAllowedHeaders(Arrays.asList("*")); // Tüm başlıklara

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Tüm yollar için
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // <-- CORS'u uygula
                .csrf(AbstractHttpConfigurer::disable) // <-- CSRF'i kapat (403'ü önler)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // OPTIONS istekleri (ön kontrol) her zaman izinli olmalı
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // GET istekleri (herkese açık)
                        .requestMatchers(HttpMethod.GET, "/api/clubs").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clubs/{clubId}").permitAll()

                        // Geri kalan tüm /api/clubs/ yolları (POST, PUT vb.)
                        .requestMatchers("/api/clubs/**").authenticated()

                        .anyRequest().authenticated()
                );

        // Filtreyi @Bean metoduyla çağırarak ekle
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}