package com.uniclubconnect.services.feedservice.service;

import com.uniclubconnect.services.feedservice.client.FollowServiceClient;
import com.uniclubconnect.services.feedservice.client.PostServiceClient;
import com.uniclubconnect.services.feedservice.dto.PostEvent;
import com.uniclubconnect.services.feedservice.dto.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final StringRedisTemplate redisTemplate;
    private final FollowServiceClient followServiceClient;
    private final PostServiceClient postServiceClient;

    private static final String FEED_KEY_PREFIX = "feed:";
    private static final int MAX_FEED_SIZE = 500; // Bir kullanıcının feed'inde en fazla 500 post tutalım

    // --- 1. YENİ POST GELDİĞİNDE (FAN-OUT PUSH) ---
    public void pushPostToFeeds(PostEvent event) {
        String postId = event.getPostId();
        String authorId = event.getAuthorId();

        // 1. Postu atanın takipçilerini bul
        List<String> followerIds = followServiceClient.getFollowers(authorId).getContent();

        // 2. Kendi feed'ine de ekle (Kendi postlarını da görsün)
        followerIds.add(authorId);

        // 3. Her bir takipçinin Redis listesine post ID'sini ekle (En başa - Left Push)
        for (String followerId : followerIds) {
            String redisKey = FEED_KEY_PREFIX + followerId;
            redisTemplate.opsForList().leftPush(redisKey, postId);

            // Feed çok şişmesin diye sondakileri kırp (Bellek optimizasyonu)
            redisTemplate.opsForList().trim(redisKey, 0, MAX_FEED_SIZE - 1);
        }
        System.out.println("Post " + postId + ", " + followerIds.size() + " kişinin akışına eklendi.");
    }

    // --- 2. POST SİLİNDİĞİNDE (REMOVE FROM FEEDS) ---
    public void removePostFromFeeds(PostEvent event) {
        String postId = event.getPostId();
        String authorId = event.getAuthorId();

        List<String> followerIds = followServiceClient.getFollowers(authorId).getContent();
        followerIds.add(authorId);

        for (String followerId : followerIds) {
            String redisKey = FEED_KEY_PREFIX + followerId;
            // Listeden o post ID'sini bul ve sil
            redisTemplate.opsForList().remove(redisKey, 0, postId);
        }
        System.out.println("Post " + postId + ", akışlardan silindi.");
    }

    // --- 3. KULLANICI AKIŞINI ÇAĞIRDIĞINDA (GET FEED) ---
    public List<PostResponse> getUserFeed(String userId, int page, int size) {
        String redisKey = FEED_KEY_PREFIX + userId;

        int start = page * size;
        int end = start + size - 1;

        // 1. Redis'ten o sayfanın Post ID'lerini çek
        List<String> postIds = redisTemplate.opsForList().range(redisKey, start, end);

        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyList(); // Akış boş
        }

        // 2. Post Service'e gidip bu ID'lerin detaylarını BATCH olarak çek
        List<PostResponse> posts = postServiceClient.getPostsByIds(postIds);

        // 3. Post Service postları ID sırasına göre dönmeyebilir.
        // Redis'teki orijinal sıraya (En yeniler en üstte) göre tekrar dizmeliyiz.
        return postIds.stream()
                .map(id -> posts.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
