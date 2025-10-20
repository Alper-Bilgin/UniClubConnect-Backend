package com.uniclubconnect.services.authservice.security.jwt;

import com.uniclubconnect.services.authservice.security.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. İstekten JWT'yi ayıkla
            String jwt = parseJwt(request);

            // 2. Token varsa ve geçerliyse
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

                // 3. Token'dan e-postayı al
                String email = jwtUtils.getEmailFromJwtToken(jwt);

                // 4. E-postayı kullanarak veritabanından kullanıcıyı (UserDetails) yükle
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // 5. Spring Security için bir 'Authentication' nesnesi oluştur
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // Parola (credential) gerekmez, token var
                        userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. Mevcut isteğin 'SecurityContext'ine bu kullanıcıyı ata
                // Artık Spring Security bu isteğin kim tarafından yapıldığını biliyor.
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Kullanıcı kimliği doğrulanamadı: {}", e.getMessage());
        }

        // 7. Filtre zincirine devam et
        filterChain.doFilter(request, response);
    }

    // İstekteki 'Authorization: Bearer <token>' başlığından token'ı ayıran metot
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7); // "Bearer " kısmını (7 karakter) at
        }

        return null;
    }
}