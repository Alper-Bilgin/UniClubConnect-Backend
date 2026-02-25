package com.uniclubconnect.services.interactionservice.service;

import com.uniclubconnect.services.interactionservice.client.EventServiceClient;
import com.uniclubconnect.services.interactionservice.client.PostServiceClient;
import com.uniclubconnect.services.interactionservice.client.ProfileServiceClient;
import com.uniclubconnect.services.interactionservice.dto.CommentResponse;
import com.uniclubconnect.services.interactionservice.dto.UserProfileDto;
import com.uniclubconnect.services.interactionservice.entity.Comment;
import com.uniclubconnect.services.interactionservice.entity.ETargetType;
import com.uniclubconnect.services.interactionservice.entity.Like;
import com.uniclubconnect.services.interactionservice.repository.CommentRepository;
import com.uniclubconnect.services.interactionservice.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InteractionService {

    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final PostServiceClient postServiceClient;
    private final EventServiceClient eventServiceClient;
    private final ProfileServiceClient profileServiceClient; // EKLENDİ

    // 1. YORUM EKLE (AYNI)
    public Comment addComment(String userId, String content, String targetId, ETargetType targetType) {
        validateTargetExists(targetId, targetType);

        Comment comment = Comment.builder()
                .userId(userId)
                .content(content)
                .targetId(targetId)
                .targetType(targetType)
                .build();

        return commentRepository.save(comment);
    }

    // 2. BEĞENİ TOGGLE (AYNI)
    @Transactional
    public String toggleLike(String userId, String targetId, ETargetType targetType) {
        validateTargetExists(targetId, targetType);

        Optional<Like> existingLike =
                likeRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            return "Beğeni geri alındı.";
        } else {
            Like like = Like.builder()
                    .userId(userId)
                    .targetId(targetId)
                    .targetType(targetType)
                    .build();

            likeRepository.save(like);
            return "Beğenildi.";
        }
    }

    // 3. YORUMLARI GETİR (DTO DÖNÜYOR - GÜNCELLENDİ)
    public List<CommentResponse> getComments(String targetId, ETargetType targetType) {
        return commentRepository
                .findByTargetIdAndTargetTypeOrderByCreatedAtDesc(targetId, targetType)
                .stream()
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList());
    }

    // 4. YORUM SİL (YENİ)
    public void deleteComment(Long commentId, String userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Yorum bulunamadı"));

        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("Bu yorumu silme yetkiniz yok.");
        }

        commentRepository.delete(comment);
    }

    // 5. BEĞENİ SAYISI (AYNI)
    public long getLikeCount(String targetId, ETargetType targetType) {
        return likeRepository.countByTargetIdAndTargetType(targetId, targetType);
    }

    // 6. KULLANICI BEĞENDİ Mİ? (YENİ - Frontend için)
    public boolean checkUserLiked(String userId, String targetId, ETargetType targetType) {
        return likeRepository
                .findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)
                .isPresent();
    }

    // TARGET VAR MI KONTROL (AYNI)
    private void validateTargetExists(String targetId, ETargetType targetType) {
        try {
            if (targetType == ETargetType.POST) {
                postServiceClient.getPostById(targetId);
            } else if (targetType == ETargetType.EVENT) {
                eventServiceClient.getEventById(Long.valueOf(targetId));
            }
        } catch (Exception e) {
            throw new RuntimeException("Hedef içerik bulunamadı veya servise erişilemiyor.");
        }
    }

    // DTO MAPPER (YENİ)
    private CommentResponse mapToCommentResponse(Comment comment) {

        String authorName = "Bilinmeyen Kullanıcı";
        String authorImage = null;

        try {
            UserProfileDto profile =
                    profileServiceClient.getProfileByAuthId(comment.getUserId());

            if (profile != null) {
                authorName = profile.getFirstName() + " " + profile.getLastName();
                authorImage = profile.getProfileImageUrl();
            }
        } catch (Exception e) {
            // Profil servisi çökerse yorumlar patlamasın
        }

        return CommentResponse.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .content(comment.getContent())
                .targetId(comment.getTargetId())
                .targetType(comment.getTargetType().name())
                .authorName(authorName)
                .authorProfileImage(authorImage)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}