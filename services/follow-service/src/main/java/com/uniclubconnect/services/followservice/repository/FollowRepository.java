package com.uniclubconnect.services.followservice.repository;

import com.uniclubconnect.services.followservice.dto.UserRecommendationProjection;
import com.uniclubconnect.services.followservice.model.Follow;
import com.uniclubconnect.services.followservice.model.FollowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import java.util.List;
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

    @Query(value =
            "SELECT f2.following_id as recommendedUserId, COUNT(f2.follower_id) as mutualCount " +
                    "FROM follow_schema.follows f1 " + // Şema adı eklendi
                    "JOIN follow_schema.follows f2 ON f1.following_id = f2.follower_id " + // Şema adı eklendi
                    "WHERE f1.follower_id = :userId " +
                    "AND f1.status = 'ACCEPTED' " +
                    "AND f2.status = 'ACCEPTED' " +
                    "AND f2.following_id != :userId " +
                    "AND f2.following_id NOT IN (SELECT following_id FROM follow_schema.follows WHERE follower_id = :userId) " + // Şema adı eklendi
                    "GROUP BY f2.following_id " +
                    "ORDER BY mutualCount DESC",
            nativeQuery = true)
    List<UserRecommendationProjection> getRecommendations(@Param("userId") String userId, Pageable pageable);
}
