package com.uniclubconnect.services.postservice.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MinioService {

    @Autowired private MinioClient minioClient;

    // YAML'da "bucketName" yazdığın için burası değişti
    @Value("${minio.bucketName}")
    private String bucketName;

    public String uploadFile(MultipartFile file) {
        try {
            // Bucket var mı kontrol et
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            // Dosya ismi oluştur
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

            // Yükle
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
}