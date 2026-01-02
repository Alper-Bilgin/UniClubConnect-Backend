package com.uniclubconnect.services.authservice.exception;

import com.uniclubconnect.services.authservice.dto.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Şifre veya Kullanıcı Adı Yanlışsa (401 Unauthorized)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> handleBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("E-posta veya şifre hatalı! Lütfen kontrol edin."));
    }

    // 2. Hesap Doğrulanmamışsa (403 Forbidden veya 401)
    // AuthService.java içinde "Hesabınız henüz doğrulanmamış" hatasını RuntimeException olarak atmıştın.
    // Onu DisabledException'a çevirirsen burası yakalar.
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<MessageResponse> handleDisabledException(DisabledException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse("Hesabınız henüz doğrulanmamış! Lütfen e-postanızı kontrol edin."));
    }

    // 3. Genel Runtime Hataları (Örn: "Email kullanımda")
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MessageResponse> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
                .badRequest()
                .body(new MessageResponse(ex.getMessage()));
    }
}