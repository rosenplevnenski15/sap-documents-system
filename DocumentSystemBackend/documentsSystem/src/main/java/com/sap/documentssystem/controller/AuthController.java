package com.sap.documentssystem.controller;

import com.sap.documentssystem.dto.LoginRequest;
import com.sap.documentssystem.dto.LoginResponse;
import com.sap.documentssystem.dto.RegisterRequest;
import com.sap.documentssystem.dto.RefreshTokenRequest;
import com.sap.documentssystem.dto.ApiResponse;
import com.sap.documentssystem.service.AuthService;
import com.sap.documentssystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.builder()
                                .message("User registered")
                                .build()
                );
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(
                authService.refreshToken(request.getRefreshToken())
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout() {

        authService.logout();

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .message("Logged out successfully")
                        .build()
        );
    }
}