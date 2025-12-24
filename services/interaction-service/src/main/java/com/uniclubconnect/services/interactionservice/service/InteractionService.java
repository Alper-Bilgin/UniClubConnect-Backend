package com.uniclubconnect.services.interactionservice.service;

import com.uniclubconnect.services.interactionservice.client.EventServiceClient;
import com.uniclubconnect.services.interactionservice.client.PostServiceClient;
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

@Service
@RequiredArgsConstructor
public class InteractionService {

    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final PostServiceClient postServiceClient;
    private final EventServiceClient eventServiceClient;

    // 1. YORUM EKLE
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

    // 2. BEĞENİ (TOGGLE: Varsa sil, yoksa ekle)
    @Transactional
    public String toggleLike(String userId, String targetId, ETargetType targetType) {
        validateTargetExists(targetId, targetType);

        Optional<Like> existingLike = likeRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);

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

    // 3. YORUMLARI GETİR
    public List<Comment> getComments(String targetId, ETargetType targetType) {
        return commentRepository.findByTargetIdAndTargetTypeOrderByCreatedAtDesc(targetId, targetType);
    }

    // 4. BEĞENİ SAYISI
    public long getLikeCount(String targetId, ETargetType targetType) {
        return likeRepository.countByTargetIdAndTargetType(targetId, targetType);
    }

    // YARDIMCI: Post/Event var mı kontrol et?
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
}
