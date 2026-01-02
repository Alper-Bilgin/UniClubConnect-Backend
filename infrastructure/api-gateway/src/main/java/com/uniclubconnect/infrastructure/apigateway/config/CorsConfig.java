package com.uniclubconnect.infrastructure.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // 1. İzin verilen kaynaklar (Frontend adresiniz)
        // React genelde 3000 veya 5173 portunda çalışır
        corsConfig.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:5173"));

        // 2. İzin verilen metotlar
        corsConfig.setMaxAge(3600L); // 1 saat önbellekte tut (Sürekli sormasın)
        corsConfig.addAllowedMethod("*"); // GET, POST, PUT, DELETE, OPTIONS

        // 3. İzin verilen başlıklar
        corsConfig.addAllowedHeader("*");

        // 4. Cookie/Credential izni (Login işlemleri için şart)
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig); // Tüm yollar için geçerli

        return new CorsWebFilter(source);
    }
}
