package com.sap.documentssystem.service;

import com.sap.documentssystem.exceptions.FileStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.UUID;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {


    private final S3Client s3Client;
    private final Executor executor = Executors.newFixedThreadPool(10);

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @PreDestroy
    public void shutdown() {
        if (executor instanceof ExecutorService es) {
            es.shutdown();
        }
    }

    public String uploadFile(MultipartFile file) {

        validateFile(file);

        String fileName = generateFileName(file);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();


            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            log.info("File uploaded successfully to S3: {}", fileName);

            return generateUrl(fileName);

        } catch (Exception ex) {
            log.error("S3 upload failed for file: {}", fileName, ex);
            throw new FileStorageException("Failed to upload file", ex);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);

            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);

            log.info("Deleted file from S3: {}", key);

        } catch (Exception ex) {
            log.error("Failed to delete file from S3: {}", fileUrl, ex);

        }
    }
    public String downloadFileAsText(String fileUrl) {
        try {
            return CompletableFuture
                    .supplyAsync(() -> downloadInternal(fileUrl), executor)
                    .get(5, TimeUnit.SECONDS);

        } catch (TimeoutException e) {
            log.error("S3 timeout for file: {}", fileUrl);
            throw new FileStorageException("S3 timeout");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FileStorageException("Thread interrupted");

        } catch (ExecutionException e) {
            log.error("S3 execution error", e);
            throw new FileStorageException("File service temporarily unavailable");
        }
    }

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new FileStorageException("File must not be empty");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new FileStorageException("File exceeds 10MB limit");
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                (!contentType.equals("application/pdf") &&
                        !contentType.equals("text/plain"))) {

            throw new FileStorageException("Only PDF and TXT files are allowed");
        }
    }

    private String generateFileName(MultipartFile file) {
        return UUID.randomUUID() + "-" + file.getOriginalFilename();
    }

    private String generateUrl(String fileName) {
        return "https://" + bucketName + ".s3.amazonaws.com/" + fileName;
    }

    private String extractKeyFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }
    private String downloadInternal(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> response =
                    s3Client.getObject(request);

            return new String(response.readAllBytes(), StandardCharsets.UTF_8);

        } catch (Exception ex) {
            throw new FileStorageException("Failed to download file from S3", ex);
        }
    }
}