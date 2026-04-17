package com.sap.documentssystem.controller;

import com.sap.documentssystem.dto.ApiResponse;
import com.sap.documentssystem.dto.UserDto;
import com.sap.documentssystem.entity.Role;
import com.sap.documentssystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeRole(
            @PathVariable UUID userId,
            @RequestParam Role role
    ) {
        userService.changeRole(userId, role);
        return ResponseEntity.ok(
                ApiResponse.builder()
                        .message("Role updated")
                        .build()
        );
    }

    @PutMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateUser(@PathVariable UUID userId) {
        userService.deactivateUser(userId);
        return ResponseEntity.ok(
                ApiResponse.builder()
                        .message("User deactivated")
                        .build()
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> activateUser(@PathVariable UUID userId) {
        userService.activateUser(userId);
        return ResponseEntity.ok(
                ApiResponse.builder()
                        .message("User activated")
                        .build()
        );
    }
}