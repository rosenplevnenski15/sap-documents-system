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
import software.amazon.awssdk.services.s3.model.*;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

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
            String key = extractKeyFromUrl(fileUrl);

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> response =
                    s3Client.getObject(request);

            return new String(response.readAllBytes());

        } catch (Exception ex) {
            throw new FileStorageException("Failed to download file");
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
}