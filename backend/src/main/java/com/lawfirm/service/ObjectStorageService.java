package com.lawfirm.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucketName:lawfirm}")
    private String bucketName;

    public String upload(String objectName, MultipartFile file) {
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return "minio:" + objectName;
        } catch (Exception e) {
            throw new RuntimeException("MinIO上传失败: " + e.getMessage(), e);
        }
    }

    public InputStream download(String storagePath) {
        try {
            String objectName = storagePath.startsWith("minio:") ? storagePath.substring("minio:".length()) : storagePath;
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO下载失败: " + e.getMessage(), e);
        }
    }

    public void delete(String storagePath) {
        try {
            String objectName = storagePath.startsWith("minio:") ? storagePath.substring("minio:".length()) : storagePath;
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.warn("MinIO删除失败: {}", e.getMessage());
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }
}
