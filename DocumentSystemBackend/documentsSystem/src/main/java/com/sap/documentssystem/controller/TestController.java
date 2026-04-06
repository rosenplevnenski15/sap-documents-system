package com.sap.documentssystem.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminEndpoint() {
        return ResponseEntity.ok(Map.of("message", "ADMIN ACCESS GRANTED"));
    }

    @GetMapping("/author")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<Map<String, String>> authorEndpoint() {
        return ResponseEntity.ok(Map.of("message", "AUTHOR ACCESS GRANTED"));
    }

    @GetMapping("/reviewer")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<Map<String, String>> reviewerEndpoint() {
        return ResponseEntity.ok(Map.of("message", "REVIEWER ACCESS GRANTED"));
    }

    @GetMapping("/reader")
    @PreAuthorize("hasRole('READER')")
    public ResponseEntity<Map<String, String>> readerEndpoint() {
        return ResponseEntity.ok(Map.of("message", "READER ACCESS GRANTED"));
    }
}