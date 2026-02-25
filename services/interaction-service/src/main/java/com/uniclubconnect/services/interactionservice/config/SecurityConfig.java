package com.uniclubconnect.services.interactionservice.config;

import com.uniclubconnect.services.interactionservice.security.AuthTokenFilter;
import com.uniclubconnect.services.interactionservice.security.jwt.JwtUtils;
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
    private JwtUtils jwtUtils;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter(jwtUtils);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // GET isteklerini serbest bırakıyoruz. (Ortada çift yıldız kullanmadan!)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/interactions/comments/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/interactions/likes/*/*/count").permitAll()

                        // Kalan her şey (Yorum yapma, beğenme, durumu sorma) token gerektirir
                        .requestMatchers("/api/interactions/**").authenticated()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
