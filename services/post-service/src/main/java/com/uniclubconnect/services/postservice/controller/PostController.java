package com.uniclubconnect.services.postservice.controller;

import com.uniclubconnect.services.postservice.dto.PostResponse;
import com.uniclubconnect.services.postservice.service.PostService;
import com.uniclubconnect.services.postservice.security.dto.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired private PostService postService;

    // 1. GÖNDERİ OLUŞTUR
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> createPost(
            @RequestParam("content") String content,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        return ResponseEntity.ok(postService.createPost(content, image, currentUser.getAuthId()));
    }

    // 2. TÜMÜNÜ LİSTELE
    @GetMapping
    public ResponseEntity<List<PostResponse>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    // 3. KULLANICI POSTLARINI LİSTELE
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostResponse>> getUserPosts(@PathVariable String userId) {
        return ResponseEntity.ok(postService.getUserPosts(userId));
    }

    // 4. TEK POST DETAYI
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable String postId) {
        return ResponseEntity.ok(postService.getPostById(postId));
    }

    // 5. GÖNDERİ SİL
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable String postId, @AuthenticationPrincipal UserPrincipal currentUser) {
        postService.deletePost(postId, currentUser.getAuthId());
        return ResponseEntity.ok().build();
    }

    // 6. GÖNDERİ GÜNCELLE
    @PutMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable String postId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        return ResponseEntity.ok(postService.updatePost(postId, content, image, currentUser.getAuthId()));
    }
}