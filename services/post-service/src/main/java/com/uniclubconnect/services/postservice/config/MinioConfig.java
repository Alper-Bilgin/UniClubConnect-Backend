package com.uniclubconnect.services.postservice.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String url;

    // YAML'da "accessKey" yazdığın için burası değişti
    @Value("${minio.accessKey}")
    private String accessKey;

    // YAML'da "secretKey" yazdığın için burası değişti
    @Value("${minio.secretKey}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }
}