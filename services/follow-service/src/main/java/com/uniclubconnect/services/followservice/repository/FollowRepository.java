package com.uniclubconnect.services.followservice.repository;

import com.uniclubconnect.services.followservice.model.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow,Long> {

    boolean existsByFollowerIdAndFollowingId(String followerId,String followingId);

    Optional<Follow> findByFollowerIdAndFollowingId(String followerId, String followingId);

    Page<Follow> findByFollowingId(String followingId, Pageable pageable);

    Page<Follow> findByFollowerId(String followerId, Pageable pageable);

    long countByFollowingId(String followingId);

    long countByFollowerId(String followerId);

}
