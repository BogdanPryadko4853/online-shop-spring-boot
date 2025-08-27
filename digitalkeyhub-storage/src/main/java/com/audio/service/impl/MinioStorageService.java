package com.audio.service.impl;

import com.audio.service.FileStorageService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.GetObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
public class MinioStorageService implements FileStorageService {
    private final MinioClient minioClient;
    private final String bucketName;

    public MinioStorageService(MinioClient minioClient, String minioBucketName) throws Exception {
        this.minioClient = minioClient;
        this.bucketName = minioBucketName;
        initBucket();
    }

    private void initBucket() throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());

        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        }
    }

    public String uploadFile(MultipartFile file, String objectName) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return objectName;
        }
    }

    public void deleteFile(String objectName) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build());
    }

    public InputStream getFile(String objectName) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build());
    }
}