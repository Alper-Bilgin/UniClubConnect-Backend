package com.uniclubconnect.services.interactionservice.controller;

import com.uniclubconnect.services.interactionservice.dto.CommentResponse;
import com.uniclubconnect.services.interactionservice.entity.Comment;
import com.uniclubconnect.services.interactionservice.entity.ETargetType;
import com.uniclubconnect.services.interactionservice.security.dto.UserPrincipal;
import com.uniclubconnect.services.interactionservice.service.InteractionService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interactions")
public class InteractionController {

    @Autowired
    private InteractionService interactionService;

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

    // 1. YORUM YAP (AYNI)
    @PostMapping("/comments")
    public ResponseEntity<Comment> addComment(
            @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        return ResponseEntity.ok(
                interactionService.addComment(
                        user.getAuthId(),
                        request.getContent(),
                        request.getTargetId(),
                        request.getTargetType()
                )
        );
    }

    // 2. YORUMLARI GETİR (DTO DÖNÜYOR - GÜNCELLENDİ)
    @GetMapping("/comments/{targetType}/{targetId}")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable ETargetType targetType,
            @PathVariable String targetId) {

        return ResponseEntity.ok(
                interactionService.getComments(targetId, targetType)
        );
    }

    // 3. YORUM SİL (YENİ)
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal user) {

        interactionService.deleteComment(commentId, user.getAuthId());
        return ResponseEntity.ok().build();
    }

    // 4. BEĞEN / VAZGEÇ (AYNI)
    @PostMapping("/likes")
    public ResponseEntity<String> toggleLike(
            @RequestBody LikeRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        return ResponseEntity.ok(
                interactionService.toggleLike(
                        user.getAuthId(),
                        request.getTargetId(),
                        request.getTargetType()
                )
        );
    }

    // 5. BEĞENİ SAYISI (AYNI)
    @GetMapping("/likes/{targetType}/{targetId}/count")
    public ResponseEntity<Long> getLikeCount(
            @PathVariable ETargetType targetType,
            @PathVariable String targetId) {

        return ResponseEntity.ok(
                interactionService.getLikeCount(targetId, targetType)
        );
    }

    // 6. KULLANICI BEĞENDİ Mİ? (YENİ)
    @GetMapping("/likes/{targetType}/{targetId}/status")
    public ResponseEntity<Boolean> checkLikeStatus(
            @PathVariable ETargetType targetType,
            @PathVariable String targetId,
            @AuthenticationPrincipal UserPrincipal user) {

        return ResponseEntity.ok(
                interactionService.checkUserLiked(
                        user.getAuthId(),
                        targetId,
                        targetType
                )
        );
    }
}