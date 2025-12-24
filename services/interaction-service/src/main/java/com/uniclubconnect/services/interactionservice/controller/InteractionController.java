package com.uniclubconnect.services.interactionservice.controller;

import com.uniclubconnect.services.interactionservice.entity.Comment;
import com.uniclubconnect.services.interactionservice.entity.ETargetType;
import com.uniclubconnect.services.interactionservice.service.InteractionService;
import com.uniclubconnect.services.interactionservice.security.dto.UserPrincipal;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interactions")
public class InteractionController {

    @Autowired private InteractionService interactionService;

    // YORUM EKLEME DTO
    @Data
    public static class CommentRequest {
        private String content;
        private String targetId;
        private ETargetType targetType;
    }

    // BEĞENİ DTO
    @Data
    public static class LikeRequest {
        private String targetId;
        private ETargetType targetType;
    }

    // 1. YORUM YAP
    @PostMapping("/comments")
    public ResponseEntity<Comment> addComment(
            @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        return ResponseEntity.ok(
                interactionService.addComment(user.getAuthId(), request.getContent(), request.getTargetId(), request.getTargetType())
        );
    }

    // 2. YORUMLARI GETİR
    @GetMapping("/comments/{targetType}/{targetId}")
    public ResponseEntity<List<Comment>> getComments(
            @PathVariable ETargetType targetType,
            @PathVariable String targetId) {
        return ResponseEntity.ok(interactionService.getComments(targetId, targetType));
    }

    // 3. BEĞEN / VAZGEÇ
    @PostMapping("/likes")
    public ResponseEntity<String> toggleLike(
            @RequestBody LikeRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        return ResponseEntity.ok(
                interactionService.toggleLike(user.getAuthId(), request.getTargetId(), request.getTargetType())
        );
    }

    // 4. BEĞENİ SAYISI
    @GetMapping("/likes/{targetType}/{targetId}/count")
    public ResponseEntity<Long> getLikeCount(
            @PathVariable ETargetType targetType,
            @PathVariable String targetId) {
        return ResponseEntity.ok(interactionService.getLikeCount(targetId, targetType));
    }
}