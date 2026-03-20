package com.sap.documentssystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Service s3Service;

    public String upload(MultipartFile file) {
        return s3Service.uploadFile(file);
    }

    public void delete(String fileUrl) {
        s3Service.deleteFile(fileUrl);
    }
}