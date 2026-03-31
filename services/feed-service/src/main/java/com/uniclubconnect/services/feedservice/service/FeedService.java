package com.uniclubconnect.services.feedservice.service;

import com.uniclubconnect.services.feedservice.client.FollowClient;
import com.uniclubconnect.services.feedservice.client.PostClient;
import com.uniclubconnect.services.feedservice.dto.FollowUserDto;
import com.uniclubconnect.services.feedservice.dto.PageResponse;
import com.uniclubconnect.services.feedservice.dto.PostEvent;
import com.uniclubconnect.services.feedservice.dto.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final StringRedisTemplate redis;
    private final FollowClient followClient;
    private final PostClient postClient;

    private static final String FEED_KEY = "feed:";

    // YENİ POST ATILDIĞINDA
    public void handlePostCreated(PostEvent event) {
        String authorId = event.getAuthorId();
        String postId = event.getPostId();

        // 1. Takipçileri çek
        PageResponse<FollowUserDto> followers = followClient.getFollowers(authorId, 0, 1000);

        if (followers.getContent() == null) return;

        // 2. followerId kullan (FIX)
        List<String> targetFeeds = followers.getContent().stream()
                .map(FollowUserDto::getFollowerId)
                .collect(Collectors.toList());

        // 3. Postu atan kullanıcıyı da ekle
        targetFeeds.add(authorId);

        // 4. Feed’lere yaz
        for (String followerId : targetFeeds) {
            String redisKey = FEED_KEY + followerId;

            // 🔹 Duplicate önleme
            redis.opsForList().remove(redisKey, 1, postId);

            // 🔹 Yeni post ekle
            redis.opsForList().leftPush(redisKey, postId);

            // 🔹 Maksimum 500 post tut
            redis.opsForList().trim(redisKey, 0, 500);

            // 🔹 TTL (7 gün)
            redis.expire(redisKey, Duration.ofDays(7));
        }
    }

    // POST SİLİNDİĞİNDE (LAZY DELETE)
    public void handlePostDeleted(PostEvent event) {
        // Bilerek boş bırakıldı.
        // Lazy delete getFeed içinde yapılır.
    }

    // FEED GETİR
    public List<PostResponse> getFeed(String userId, int page, int size) {
        int start = page * size;
        int end = start + size - 1;

        // 1. Redis'ten ID'leri çek
        List<String> postIds = redis.opsForList().range(FEED_KEY + userId, start, end);

        if (postIds == null || postIds.isEmpty()) return List.of();

        // 2. Post servisinden içerikleri çek
        List<PostResponse> posts = postClient.getPostsByIds(postIds);

        // 🔹 O(N²) → O(N) fix (Map kullanımı)
        Map<String, PostResponse> postMap = posts.stream()
                .collect(Collectors.toMap(PostResponse::getId, p -> p));

        // 3. Lazy delete filtreleme
        return postIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}