package com.uniclubconnect.services.clubservice.controller;

import com.uniclubconnect.services.clubservice.exception.AlreadyMemberException;
import com.uniclubconnect.services.clubservice.exception.ClubNotFoundException;
import com.uniclubconnect.services.clubservice.exception.RequestNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Tüm Controller'lar için merkezi hata yönetimi
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - Kaynak Bulunamadı Hataları
    @ExceptionHandler({ClubNotFoundException.class, RequestNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFoundException(RuntimeException ex) {
        return Map.of("error", ex.getMessage());
    }

    // 400 - Kötü İstek (Kullanıcı Hatası)
    @ExceptionHandler({AlreadyMemberException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequestException(RuntimeException ex) {
        return Map.of("error", ex.getMessage());
    }

    // 403 - Yetki Hatası (@PreAuthorize'dan gelen)
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleAccessDenied(AccessDeniedException ex) {
        return Map.of("error", "Bu işlemi yapmak için yetkiniz bulunmamaktadır.");
    }

    // 500 - Beklenmedik Sunucu Hataları (MinIO vb.)
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleInternalError(RuntimeException ex) {
        ex.printStackTrace(); // Loglama için
        return Map.of("error", "Beklenmedik bir sunucu hatası oluştu: " + ex.getMessage());
    }
}