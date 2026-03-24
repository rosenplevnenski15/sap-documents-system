package com.sap.documentssystem.controller;

import com.sap.documentssystem.dto.LoginRequest;
import com.sap.documentssystem.dto.LoginResponse;
import com.sap.documentssystem.dto.RegisterRequest;
import com.sap.documentssystem.service.AuthService;
import com.sap.documentssystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
       return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "User registered"));
    }
}