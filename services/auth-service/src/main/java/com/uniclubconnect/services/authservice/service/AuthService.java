package com.uniclubconnect.services.authservice.service;

import com.uniclubconnect.services.authservice.dto.AuthResponse;
import com.uniclubconnect.services.authservice.dto.LoginRequest;
import com.uniclubconnect.services.authservice.dto.RegisterRequest;
import com.uniclubconnect.services.authservice.entity.ERole;
import com.uniclubconnect.services.authservice.entity.Role;
import com.uniclubconnect.services.authservice.entity.User;
import com.uniclubconnect.services.authservice.event.UserCreatedEvent;
import com.uniclubconnect.services.authservice.event.UserEventPublisher;
import com.uniclubconnect.services.authservice.repository.RoleRepository;
import com.uniclubconnect.services.authservice.repository.UserRepository;
import com.uniclubconnect.services.authservice.security.jwt.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserEventPublisher userEventPublisher;

    @Transactional
    public User registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Hata: E-posta adresi zaten kullanımda!");
        }

        // 1. Yeni User nesnesi oluştur
        User user = new User(
                registerRequest.getEmail(),
                passwordEncoder.encode(registerRequest.getPassword())
        );

        // 2. Varsayılan rolü ata (ROLE_USER)
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Hata: ROLE_USER rolü bulunamadı."));
        roles.add(userRole);
        user.setRoles(roles);

        // 3. Kullanıcıyı veritabanına kaydet
        User savedUser = userRepository.save(user);

        // 4. RabbitMQ ile 'user.created' olayını yayınla
        // (profile-service'in profili oluşturması için)
        UserCreatedEvent event = new UserCreatedEvent(
                savedUser.getId(),
                savedUser.getEmail(),
                registerRequest.getFirstName(),
                registerRequest.getLastName()
        );
        userEventPublisher.publishUserCreated(event);

        return savedUser;
    }

    public AuthResponse loginUser(LoginRequest loginRequest) {
        // 1. Spring Security'e 'email' ve 'password'u doğrulat
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        // 2. Kimliği SecurityContext'e yerleştir
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. JWT token'ları üret
        String accessToken = jwtUtils.generateJwtToken(authentication);
        String refreshToken = jwtUtils.generateRefreshToken(authentication);

        // 4. Kullanıcı detaylarını al
        User userDetails = (User) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // 5. AuthResponse DTO'sunu oluşturup dön
        return new AuthResponse(
                accessToken,
                refreshToken,
                userDetails.getId(),
                userDetails.getEmail(),
                roles
        );
    }
}