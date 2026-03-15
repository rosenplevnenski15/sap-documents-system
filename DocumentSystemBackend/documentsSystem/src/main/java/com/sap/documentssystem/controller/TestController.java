package com.sap.documentssystem.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminEndpoint() {
        return "ADMIN ACCESS GRANTED";
    }

    @GetMapping("/api/test/author")
    @PreAuthorize("hasRole('AUTHOR')")
    public String authorEndpoint() {
        return "AUTHOR ACCESS GRANTED";
    }

    @GetMapping("/api/test/reviewer")
    @PreAuthorize("hasRole('REVIEWER')")
    public String reviewerEndpoint() {
        return "REVIEWER ACCESS GRANTED";
    }

    @GetMapping("/api/test/reader")
    @PreAuthorize("hasRole('READER')")
    public String readerEndpoint() {
        return "READER ACCESS GRANTED";
    }

}