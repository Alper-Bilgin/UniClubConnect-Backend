package com.uniclubconnect.services.clubservice.service;

import io.minio.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MinioService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${minio.url}")
    private String minioUrl;

    @PostConstruct
    public void init() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                System.out.println("Bucket oluşturuldu: " + bucketName);
            }
            // Her seferinde Public Policy ayarını zorla
            setBucketPublicPolicy();
        } catch (Exception e) {
            throw new RuntimeException("MinIO başlatma hatası: " + e.getMessage());
        }
    }

    private void setBucketPublicPolicy() {
        try {
            String policyConfig = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": { "AWS": ["*"] },
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucketName);

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(policyConfig)
                            .build()
            );
            System.out.println("Bucket erişim izni PUBLIC yapıldı: " + bucketName);
        } catch (Exception e) {
            System.err.println("Bucket policy ayarlanamadı: " + e.getMessage());
        }
    }

    public String uploadFile(MultipartFile file) {
        try {
            // Dosya isminin çakışmaması için timestamp ekliyoruz
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Resim yükleme hatası: " + e.getMessage());
        }
    }

    public String getFileUrl(String fileName) {
        if (fileName == null || fileName.isEmpty()) return null;
        return minioUrl + "/" + bucketName + "/" + fileName;
    }
}