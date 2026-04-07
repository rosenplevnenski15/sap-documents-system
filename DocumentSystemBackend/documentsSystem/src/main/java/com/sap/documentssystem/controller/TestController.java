package com.sap.documentssystem.controller;

import com.sap.documentssystem.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> adminEndpoint() {
        return ResponseEntity.ok(
                ApiResponse.builder()
                        .message("ADMIN ACCESS GRANTED")
                        .build()
        );
    }

    @GetMapping("/author")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<ApiResponse> authorEndpoint() {
        return ResponseEntity.ok(
                ApiResponse.builder()
                        .message("AUTHOR ACCESS GRANTED")
                        .build()
        );
    }

    @GetMapping("/reviewer")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<ApiResponse> reviewerEndpoint() {
        return ResponseEntity.ok(
                ApiResponse.builder()
                        .message("REVIEWER ACCESS GRANTED")
                        .build()
        );
    }

    @GetMapping("/reader")
    @PreAuthorize("hasRole('READER')")
    public ResponseEntity<ApiResponse> readerEndpoint() {
        return ResponseEntity.ok(
                ApiResponse.builder()
                        .message("READER ACCESS GRANTED")
                        .build()
        );
    }
}