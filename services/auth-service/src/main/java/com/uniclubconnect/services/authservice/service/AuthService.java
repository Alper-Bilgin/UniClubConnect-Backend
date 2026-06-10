package com.uniclubconnect.services.authservice.service;

import com.uniclubconnect.services.authservice.dto.AuthResponse;
import com.uniclubconnect.services.authservice.dto.LoginRequest;
import com.uniclubconnect.services.authservice.dto.PasswordResetEvent;
import com.uniclubconnect.services.authservice.dto.RegisterRequest;
import com.uniclubconnect.services.authservice.dto.ResetPasswordRequest;
import com.uniclubconnect.services.authservice.dto.RoleUpgradeRequestResponse;
import com.uniclubconnect.services.authservice.entity.ERole;
import com.uniclubconnect.services.authservice.entity.ERoleRequestStatus;
import com.uniclubconnect.services.authservice.entity.Role;
import com.uniclubconnect.services.authservice.entity.RoleUpgradeRequest;
import com.uniclubconnect.services.authservice.entity.User;
import com.uniclubconnect.services.authservice.event.UserCreatedEvent;
import com.uniclubconnect.services.authservice.repository.RoleRepository;
import com.uniclubconnect.services.authservice.repository.RoleUpgradeRequestRepository;
import com.uniclubconnect.services.authservice.repository.UserRepository;
import com.uniclubconnect.services.authservice.security.jwt.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoleUpgradeRequestRepository roleUpgradeRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    // ESKİ UserEventPublisher REFERANSI SİLİNDİ

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${auth.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${auth.rabbitmq.routing-key}")
    private String routingKey;

    @Value("${gamification.rabbitmq.exchange}")
    private String gamificationExchange;

    @Value("${gamification.rabbitmq.routing-key}")
    private String gamificationRoutingKey;

    // ----------------------------------------------------------------
    //  1. KULLANICI KAYIT
    // ----------------------------------------------------------------

    @Transactional
    public User registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Hata: E-posta adresi zaten kullanımda!");
        }

        User user = new User(
                registerRequest.getEmail(),
                passwordEncoder.encode(registerRequest.getPassword()),
                registerRequest.getFirstName(),
                registerRequest.getLastName()
        );

        // Kullanıcı PASİF başlar
        user.setEnabled(false);

        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Hata: ROLE_USER rolü bulunamadı."));
        user.setRoles(Set.of(userRole));

        User savedUser = userRepository.save(user);

        // Doğrulama Kodu Üret
        String verificationCode = String.valueOf(new Random().nextInt(900000) + 100000);

        // Redis'e Yaz (5 dk)
        redisTemplate.opsForValue().set(
                "verify:" + savedUser.getEmail(),
                verificationCode,
                5, TimeUnit.MINUTES
        );

        // Event Oluştur
        UserCreatedEvent event = new UserCreatedEvent(
                savedUser.getId(),
                savedUser.getEmail(),
                registerRequest.getFirstName(),
                registerRequest.getLastName(),
                verificationCode
        );

        // RabbitMQ Mesajı Gönder
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
            logger.info("RabbitMQ mesajı gönderildi: {}", savedUser.getEmail());
        } catch (Exception e) {
            logger.error("RabbitMQ hatası: {}", e.getMessage());
        }

        logger.info("Yeni kullanıcı kaydedildi: {}", savedUser.getEmail());
        return savedUser;
    }

    // ----------------------------------------------------------------
    //  2. KULLANICI DOĞRULAMA (VERIFY)
    // ----------------------------------------------------------------

    public String verifyUser(String email, String code) {
        String redisKey = "verify:" + email;
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            throw new RuntimeException("Doğrulama kodunun süresi dolmuş veya kod geçersiz.");
        }

        if (!storedCode.equals(code)) {
            throw new RuntimeException("Hatalı doğrulama kodu!");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + email));

        user.setEnabled(true);
        userRepository.save(user);
        redisTemplate.delete(redisKey);

        logger.info("Kullanıcı doğrulandı: {}", email);
        return "Hesabınız başarıyla doğrulandı! Giriş yapabilirsiniz.";
    }


    @Transactional
    public String resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        if (user.isEnabled()) {
            throw new RuntimeException("Bu hesap zaten doğrulanmış. Giriş yapabilirsiniz.");
        }

        // Yeni kod üret
        String verificationCode = String.valueOf(new Random().nextInt(900000) + 100000);

        // Redis'e üzerine yaz (Süreyi 5 dk'dan tekrar başlatır)
        redisTemplate.opsForValue().set(
                "verify:" + user.getEmail(),
                verificationCode,
                5, TimeUnit.MINUTES
        );

        // RabbitMQ'ya tekrar mesaj at (Notification servisi yine mail atacak)
        // Not: firstName/lastName'i veritabanından aldık
        UserCreatedEvent event = new UserCreatedEvent(
                user.getId(),
                user.getEmail(),
                user.getFirstName(), // DB'den gelen isim
                user.getLastName(),
                verificationCode
        );

        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
        } catch (Exception e) {
            logger.error("RabbitMQ hatası: {}", e.getMessage());
        }

        return "Yeni doğrulama kodu e-posta adresinize gönderildi.";
    }

    // ----------------------------------------------------------------
    //  3. GİRİŞ (LOGIN)
    // ----------------------------------------------------------------

    public AuthResponse loginUser(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String accessToken = jwtUtils.generateJwtToken(authentication);
            String refreshToken = jwtUtils.generateRefreshToken(authentication);

            User userDetails = (User) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // 👇👇 YENİ EKLENEN KISIM: Başarılı girişi Gamification'a bildir 👇👇
            try {
                com.uniclubconnect.services.authservice.dto.GamificationEvent event =
                        com.uniclubconnect.services.authservice.dto.GamificationEvent.builder()
                                .userId(userDetails.getId())
                                .eventType(com.uniclubconnect.services.authservice.dto.EventType.USER_LOGIN)
                                .timestamp(LocalDateTime.now())
                                .build();

                rabbitTemplate.convertAndSend(gamificationExchange, gamificationRoutingKey, event);
                logger.info("Gamification eventi (USER_LOGIN) fırlatıldı: {}", userDetails.getEmail());
            } catch (Exception e) {
                logger.error("Gamification'a mesaj gönderilirken hata oluştu: {}", e.getMessage());
            }
            // 👆👆 YENİ EKLENEN KISIM BİTTİ 👆👆

            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    userDetails.getId(),
                    userDetails.getEmail(),
                    roles
            );

        } catch (DisabledException e) {
            throw new DisabledException("Hesap doğrulanmadı");
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Hatalı giriş");
        }
    }

    // ----------------------------------------------------------------
    //  4. ROL İŞLEMLERİ (Aynen kaldı)
    // ----------------------------------------------------------------

    @Transactional
    public RoleUpgradeRequestResponse requestClubOwnerRole(String requestingUserId) {
        User user = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new UserNotFoundException("Kullanıcı bulunamadı: " + requestingUserId));

        boolean alreadyClubOwner = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(ERole.ROLE_CLUB_OWNER));
        if (alreadyClubOwner) {
            throw new IllegalStateException("Kullanıcı zaten Kulüp Sahibi rolüne sahip.");
        }

        boolean existingRequest = roleUpgradeRequestRepository.existsByRequestingUserAndStatusIn(
                user, List.of(ERoleRequestStatus.PENDING, ERoleRequestStatus.APPROVED)
        );
        if (existingRequest) {
            throw new IllegalStateException("Zaten aktif bir rol yükseltme isteğiniz mevcut.");
        }

        RoleUpgradeRequest newRequest = RoleUpgradeRequest.builder()
                .requestingUser(user)
                .status(ERoleRequestStatus.PENDING)
                .build();
        RoleUpgradeRequest savedRequest = roleUpgradeRequestRepository.save(newRequest);

        logger.info("Yeni rol yükseltme isteği: {}", user.getEmail());
        return mapToRequestResponseDTO(savedRequest);
    }

    public RoleUpgradeRequestResponse getCurrentUserRoleRequestStatus(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Kullanıcı bulunamadı: " + userId));

        return roleUpgradeRequestRepository.findByRequestingUser(user)
                .map(this::mapToRequestResponseDTO)
                .orElse(null);
    }

    public List<RoleUpgradeRequestResponse> getPendingRoleRequests() {
        return roleUpgradeRequestRepository.findByStatus(ERoleRequestStatus.PENDING).stream()
                .map(this::mapToRequestResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleUpgradeRequestResponse approveRoleRequest(Long requestId, String adminUserId) {
        RoleUpgradeRequest request = roleUpgradeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("İstek bulunamadı: " + requestId));

        if (request.getStatus() != ERoleRequestStatus.PENDING) {
            throw new IllegalStateException("İstek zaten sonuçlanmış.");
        }

        User targetUser = request.getRequestingUser();
        Role clubOwnerRole = roleRepository.findByName(ERole.ROLE_CLUB_OWNER)
                .orElseThrow(() -> new RuntimeException("Hata: ROLE_CLUB_OWNER rolü bulunamadı."));

        targetUser.getRoles().add(clubOwnerRole);
        userRepository.save(targetUser);

        request.setStatus(ERoleRequestStatus.APPROVED);
        request.setReviewedByAdminId(adminUserId);
        request.setResolutionDate(LocalDateTime.now());
        RoleUpgradeRequest updatedRequest = roleUpgradeRequestRepository.save(request);

        logger.info("Rol isteği onaylandı: {}", targetUser.getEmail());
        return mapToRequestResponseDTO(updatedRequest);
    }

    @Transactional
    public RoleUpgradeRequestResponse rejectRoleRequest(Long requestId, String adminUserId) {
        RoleUpgradeRequest request = roleUpgradeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("İstek bulunamadı: " + requestId));

        if (request.getStatus() != ERoleRequestStatus.PENDING) {
            throw new IllegalStateException("İstek zaten sonuçlanmış.");
        }

        request.setStatus(ERoleRequestStatus.REJECTED);
        request.setReviewedByAdminId(adminUserId);
        request.setResolutionDate(LocalDateTime.now());
        RoleUpgradeRequest updatedRequest = roleUpgradeRequestRepository.save(request);

        logger.info("Rol isteği reddedildi ID: {}", requestId);
        return mapToRequestResponseDTO(updatedRequest);
    }

    private RoleUpgradeRequestResponse mapToRequestResponseDTO(RoleUpgradeRequest request) {
        String adminEmail = null;
        if (request.getReviewedByAdminId() != null) {
            adminEmail = userRepository.findById(request.getReviewedByAdminId())
                    .map(User::getEmail)
                    .orElse("Bilinmeyen Admin");
        }

        return RoleUpgradeRequestResponse.builder()
                .id(request.getId())
                .requestingUserEmail(request.getRequestingUser().getEmail())
                .status(request.getStatus())
                .requestDate(request.getRequestDate())
                .resolutionDate(request.getResolutionDate())
                .reviewedByAdminEmail(adminEmail)
                .build();
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    public static class RequestNotFoundException extends RuntimeException {
        public RequestNotFoundException(String message) {
            super(message);
        }
    }

    // ----------------------------------------------------------------
    //  5. ŞİFRE SIFIRLAMA
    // ----------------------------------------------------------------

    @Transactional
    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Bu e-posta adresiyle kayıtlı kullanıcı bulunamadı."));

        // 6 Haneli şifre sıfırlama kodu üret
        String resetCode = String.valueOf(new Random().nextInt(900000) + 100000);

        // Redis'e kaydet (15 Dakika geçerli)
        redisTemplate.opsForValue().set(
                "reset:" + user.getEmail(),
                resetCode,
                15, TimeUnit.MINUTES
        );

        // RabbitMQ'ya Event fırlat (Notification Service bunu dinleyip mail atacak)
        PasswordResetEvent event = new PasswordResetEvent(
                user.getEmail(),
                resetCode,
                user.getFirstName()
        );

        try {
            rabbitTemplate.convertAndSend(exchangeName, "user.reset.key", event); // routing-key'i "user.reset.key" yapabilirsin
            logger.info("Şifre sıfırlama maili tetiklendi: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("RabbitMQ hatası (Şifre Sıfırlama): {}", e.getMessage());
        }

        return "Şifre sıfırlama kodu e-posta adresinize gönderildi (15 dakika geçerlidir).";
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        String redisKey = "reset:" + request.getEmail();
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            throw new RuntimeException("Şifre sıfırlama kodunun süresi dolmuş veya geçersiz.");
        }

        if (!storedCode.equals(request.getCode())) {
            throw new RuntimeException("Hatalı sıfırlama kodu!");
        }

        // Kodu doğruladık, şifreyi değiştirelim
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Kullanılmış kodu Redis'ten sil
        redisTemplate.delete(redisKey);

        logger.info("Kullanıcı şifresini başarıyla sıfırladı: {}", request.getEmail());
        return "Şifreniz başarıyla güncellendi. Yeni şifrenizle giriş yapabilirsiniz.";
    }
}