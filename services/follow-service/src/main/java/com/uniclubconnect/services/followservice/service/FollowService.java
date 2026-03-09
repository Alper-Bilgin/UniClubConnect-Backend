package com.uniclubconnect.services.followservice.service;

import com.uniclubconnect.services.followservice.config.RabbitMQConfig;
import com.uniclubconnect.services.followservice.dto.FollowEvent;
import com.uniclubconnect.services.followservice.model.Follow;
import com.uniclubconnect.services.followservice.repository.FollowRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redis;

    private static final String FOLLOWER_COUNT="user:follower_count:";
    private static final String FOLLOWING_COUNT="user:following_count:";

    @Transactional
    public void followUser(String followerId,String targetId){

        if(followerId.equals(targetId))
            throw new IllegalArgumentException("Self follow not allowed");

        if(repository.existsByFollowerIdAndFollowingId(followerId,targetId))
            return;

        Follow follow=Follow.builder()
                .followerId(followerId)
                .followingId(targetId)
                .build();

        repository.save(follow);

        redis.delete(FOLLOWER_COUNT+targetId);
        redis.delete(FOLLOWING_COUNT+followerId);

        FollowEvent event=FollowEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .followerId(followerId)
                .followingId(targetId)
                .type("FOLLOW_CREATED")
                .timestamp(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                event
        );

    }

    @Transactional
    public void unfollowUser(String followerId, String targetId){

        Optional<Follow> followOpt =
                repository.findByFollowerIdAndFollowingId(followerId,targetId);

        if(followOpt.isEmpty()){
            return;
        }

        repository.delete(followOpt.get());

        // Cache invalidate
        redis.delete(FOLLOWER_COUNT + targetId);
        redis.delete(FOLLOWING_COUNT + followerId);

        // Event publish
        FollowEvent event = FollowEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .followerId(followerId)
                .followingId(targetId)
                .type("FOLLOW_DELETED")
                .timestamp(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                event
        );
    }

    public boolean isFollowing(String followerId,String targetId){
        return repository.existsByFollowerIdAndFollowingId(followerId,targetId);
    }

    public Map<String,Long> getStats(String userId) {
        String fKey = FOLLOWER_COUNT + userId;
        String gKey = FOLLOWING_COUNT + userId;

        String fVal = redis.opsForValue().get(fKey);
        String gVal = redis.opsForValue().get(gKey);

        // Sadece null olanları veritabanından çek ve kaydet
        long followers = (fVal != null) ? Long.parseLong(fVal) : fetchAndCache(fKey, () -> repository.countByFollowingId(userId));
        long following = (gVal != null) ? Long.parseLong(gVal) : fetchAndCache(gKey, () -> repository.countByFollowerId(userId));

        return Map.of(
                "followers", followers,
                "following", following
        );
    }

    // DRY (Don't Repeat Yourself) prensibi için yardımcı metod
    private long fetchAndCache(String key, Supplier<Long> dbQuery) {
        long count = dbQuery.get();
        redis.opsForValue().set(key, String.valueOf(count));
        return count;
    }

    // --- LİSTELEME (SAYFALI) ---
    public Page<String> getFollowers(String userId, Pageable pageable) {
        // Entity yerine direkt String ID'leri dönüyoruz
        return repository.findByFollowingId(userId, pageable)
                .map(Follow::getFollowerId);
    }

    public Page<String> getFollowing(String userId, Pageable pageable) {
        return repository.findByFollowerId(userId, pageable)
                .map(Follow::getFollowingId);
    }
}