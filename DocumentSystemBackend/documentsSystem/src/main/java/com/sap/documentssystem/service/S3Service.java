package com.sap.documentssystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    private final String bucketName = "sap-s3-proj-buck";

    public String uploadFile(String fileName, Path filePath) {

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        s3Client.putObject(putObjectRequest, filePath);

        return "https://" + bucketName + ".s3.amazonaws.com/" + fileName;
    }

}