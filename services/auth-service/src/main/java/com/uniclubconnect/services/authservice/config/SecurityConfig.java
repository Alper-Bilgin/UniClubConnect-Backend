package com.uniclubconnect.services.authservice.config;

import com.uniclubconnect.services.authservice.security.jwt.AuthTokenFilter;
import com.uniclubconnect.services.authservice.security.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity // @PreAuthorize gibi anotasyonları aktif eder (Admin rolleri için)
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    // Token filtresini (bir sonraki adımda yazacağız)
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    // Spring'e, kullanıcıları veritabanından nasıl çekeceğini (UserDetailsService)
    // ve şifreleri nasıl karşılaştıracağını (PasswordEncoder) söyler.
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // Login işlemi için gereken AuthenticationManager'ı 'Bean' olarak sunar.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // Şifreleme algoritması: BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Tüm HTTP güvenlik kurallarının zinciri
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF'i devre dışı bırak (JWT kullandığımız için gerek yok)
                .csrf(AbstractHttpConfigurer::disable)

                // FORM TABANLI GİRİŞİ KAPAT (ASIL ÇÖZÜM BU)
                .formLogin(AbstractHttpConfigurer::disable)

                // HTTP BASIC (Popup) GİRİŞİNİ KAPAT
                .httpBasic(AbstractHttpConfigurer::disable)

                // JWT kullandığımız için oturum (Session) yönetimini STATELESS yap
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Gelen isteklere izin kuralları
                .authorizeHttpRequests(auth -> auth
                        // /api/auth/ ile başlayan TÜM isteklere (login, register) izinsiz erişime izin ver
                        .requestMatchers("/api/auth/**").permitAll()
                        // Admin ve Kullanıcı istek yolları kimlik doğrulaması gerektirir
                        .requestMatchers("/api/admin/**").authenticated()
                        .requestMatchers("/api/requests/**").authenticated()
                        // Geri kalan tüm istekler kimlik doğrulaması (Authentication) gerektirir
                        .anyRequest().authenticated()
                );

        // Kendi AuthProvider'ımızı (DB'den kullanıcı çeken) Spring'e tanıt
        http.authenticationProvider(authenticationProvider());

        // Kendi yazdığımız token filtresini, Spring'in ana filtresinden ÖNCE çalıştır
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}