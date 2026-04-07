package com.sap.documentssystem.controller;

import com.sap.documentssystem.dto.HealthResponse;
import com.sap.documentssystem.dto.HomeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<HomeResponse> home() {
        return ResponseEntity.ok(
                HomeResponse.builder()
                        .application("Documents System API")
                        .status("RUNNING")
                        .build()
        );
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(
                HealthResponse.builder()
                        .status("UP")
                        .build()
        );
    }
}