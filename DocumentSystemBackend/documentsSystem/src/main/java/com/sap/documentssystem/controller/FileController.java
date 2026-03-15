package com.sap.documentssystem.controller;

import com.sap.documentssystem.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final S3Service s3Service;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    public String uploadFile(@RequestParam MultipartFile file) throws Exception {

        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

        Path tempFile = Files.createTempFile("upload-", fileName);
        Files.write(tempFile, file.getBytes());

        String url = s3Service.uploadFile(fileName, tempFile);

        Files.delete(tempFile);

        return url;
    }
}