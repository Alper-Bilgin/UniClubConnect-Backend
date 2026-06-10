package com.uniclubconnect.services.authservice.controller;

import com.uniclubconnect.services.authservice.dto.AuthResponse;
import com.uniclubconnect.services.authservice.dto.ForgotPasswordRequest;
import com.uniclubconnect.services.authservice.dto.LoginRequest;
import com.uniclubconnect.services.authservice.dto.MessageResponse;
import com.uniclubconnect.services.authservice.dto.RegisterRequest;
import com.uniclubconnect.services.authservice.dto.ResetPasswordRequest;
import com.uniclubconnect.services.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth") // SecurityConfig'de bu yola izin vermiştik
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/test")
    public ResponseEntity<String> getTestEndpoint() {
        // Bu metot, SecurityConfig'deki "/api/auth/**" kuralı sayesinde
        // herkese (permitAll) açık olacaktır.
        return ResponseEntity.ok("Auth Service GET Test Endpoint CALISIYOR!");
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            authService.registerUser(registerRequest);
            return ResponseEntity.ok(new MessageResponse("Kayıt başarılı! Lütfen e-postanıza gelen kodu doğrulayın."));
        } catch (RuntimeException e) {
            // E-posta zaten kullanımda hatası
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        // try-catch YOK! Doğrudan servisi çağırıyoruz.
        AuthResponse authResponse = authService.loginUser(loginRequest);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam String email, @RequestParam String code) {
        try {
            String result = authService.verifyUser(email, code);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend-code")
    public ResponseEntity<?> resendCode(@RequestParam String email) {
        try {
            String result = authService.resendVerificationCode(email);
            return ResponseEntity.ok(new MessageResponse(result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            String result = authService.forgotPassword(request.getEmail());
            return ResponseEntity.ok(new MessageResponse(result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            String result = authService.resetPassword(request);
            return ResponseEntity.ok(new MessageResponse(result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

}