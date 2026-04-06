package com.sap.documentssystem.controller;

import com.sap.documentssystem.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    public ResponseEntity<String> uploadFile(@RequestParam MultipartFile file) {

        String url = fileService.upload(file);

        return ResponseEntity.status(HttpStatus.CREATED).body(url);
    }
}