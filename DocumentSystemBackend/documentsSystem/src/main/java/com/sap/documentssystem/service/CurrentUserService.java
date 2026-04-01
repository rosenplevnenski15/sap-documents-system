package com.sap.documentssystem.service;

import com.sap.documentssystem.entity.User;
import com.sap.documentssystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        String username = extractUsername();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public String getCurrentUsername() {
        return extractUsername();
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    private String extractUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new UsernameNotFoundException("Authenticated user not found");
        }

        return auth.getName();
    }
}