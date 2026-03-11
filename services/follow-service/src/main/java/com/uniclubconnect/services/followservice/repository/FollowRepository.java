package com.uniclubconnect.services.followservice.repository;

import com.uniclubconnect.services.followservice.model.Follow;
import com.uniclubconnect.services.followservice.model.FollowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerIdAndFollowingId(String followerId, String followingId);

    boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);

    boolean existsByFollowerIdAndFollowingIdAndStatus(String followerId, String followingId, FollowStatus status);

    Page<Follow> findByFollowingIdAndStatus(String followingId, FollowStatus status, Pageable pageable);

    Page<Follow> findByFollowerIdAndStatus(String followerId, FollowStatus status, Pageable pageable);

    long countByFollowingIdAndStatus(String followingId, FollowStatus status);

    long countByFollowerIdAndStatus(String followerId, FollowStatus status);

    void deleteByFollowerIdAndFollowingIdAndStatus(String followerId, String followingId, FollowStatus status);
}
