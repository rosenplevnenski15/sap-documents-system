package com.sap.documentssystem.controller;

import com.sap.documentssystem.dto.UserDto;
import com.sap.documentssystem.entity.Role;
import com.sap.documentssystem.entity.User;
import com.sap.documentssystem.repository.UserRepository;
import com.sap.documentssystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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
        return ResponseEntity.ok(Map.of("message", "Role updated"));
    }

    @PutMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateUser(@PathVariable UUID userId) {
        userService.deactivateUser(userId);
        return ResponseEntity.ok(Map.of("message", "User deactivated"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> getAllUsers() {
        return userService.getAllUsers();
    }
}