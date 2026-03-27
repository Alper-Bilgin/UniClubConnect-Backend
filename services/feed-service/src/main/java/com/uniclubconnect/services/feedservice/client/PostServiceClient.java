package com.uniclubconnect.services.feedservice.client;

import com.uniclubconnect.services.feedservice.dto.PostResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "post-service")
public interface PostServiceClient {

    // 🚨 KRİTİK: Post servisinde birden fazla postu ID listesi ile çekebileceğimiz bir "BATCH" endpointine ihtiyacımız var!
    // Aksi halde 20 post için Post servisine 20 kere HTTP isteği atarız, sistem çöker.
    // Post Service'e gidip bu endpoint'i eklemeliyiz (Nasıl ekleneceğini aşağıda yazdım).
    @PostMapping("/api/posts/batch")
    List<PostResponse> getPostsByIds(@RequestBody List<String> postIds);
}
