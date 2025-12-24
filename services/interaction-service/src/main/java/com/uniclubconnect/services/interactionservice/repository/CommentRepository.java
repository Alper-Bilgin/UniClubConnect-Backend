package com.uniclubconnect.services.interactionservice.repository;

import com.uniclubconnect.services.interactionservice.entity.Comment;
import com.uniclubconnect.services.interactionservice.entity.ETargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTargetIdAndTargetTypeOrderByCreatedAtDesc(String targetId, ETargetType targetType);
}
