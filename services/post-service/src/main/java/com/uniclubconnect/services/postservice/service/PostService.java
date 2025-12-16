package com.uniclubconnect.services.postservice.service;

import com.uniclubconnect.services.postservice.dto.PostResponse;
import com.uniclubconnect.services.postservice.entity.Post;
import com.uniclubconnect.services.postservice.exception.PostNotFoundException;
import com.uniclubconnect.services.postservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MinioService minioService;

    public PostResponse createPost(String content, MultipartFile image, String userId) {
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = minioService.uploadFile(image);
        }

        Post post = Post.builder().userId(userId).content(content).imageUrl(imageUrl).build();
        return mapToResponse(postRepository.save(post));
    }

    public List<PostResponse> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<PostResponse> getUserPosts(String userId) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public PostResponse getPostById(String postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException("Gönderi bulunamadı: " + postId));
        return mapToResponse(post);
    }

    public void deletePost(String postId, String userId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException("Gönderi bulunamadı"));
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("Bu gönderiyi silme yetkiniz yok!");
        }
        postRepository.delete(post);
    }

    private PostResponse mapToResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .createdAt(post.getCreatedAt())
                .build();
    }
}