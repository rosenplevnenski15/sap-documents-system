package com.sap.documentssystem.service;
import com.sap.documentssystem.dto.RegisterRequest;
import com.sap.documentssystem.dto.UserDto;
import com.sap.documentssystem.exceptions.UserAlreadyExistsException;
import com.sap.documentssystem.exceptions.UserAlreadyInActiveException;
import com.sap.documentssystem.exceptions.UserNotFoundException;
import com.sap.documentssystem.entity.AuditAction;
import com.sap.documentssystem.entity.Role;
import com.sap.documentssystem.entity.User;
import com.sap.documentssystem.mapper.MapUser;
import com.sap.documentssystem.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    public void register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException();
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.READER)
                .isActive(true)
                .build();

        userRepository.save(user);

        auditLogService.log(
                user,
                AuditAction.USER_REGISTERED,
                "USER",
                user.getId(),
                Map.of(
                        "username", user.getUsername(),
                        "role", user.getRole().name(),
                        "isActive", user.isActive()
                )
        );
    }
    @Transactional
    public void changeRole(UUID userId, Role newRole) {

        User admin = currentUserService.getCurrentUser();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Role oldRole = user.getRole();
        if (user.getRole() == newRole) {
            throw new IllegalArgumentException("User already has this role");
        }

        user.setRole(newRole);

        auditLogService.log(
                admin,
                AuditAction.USER_ROLE_CHANGED,
                "USER",
                userId,
                Map.of(
                        "targetUserId", userId,
                        "oldRole", oldRole.name(),
                        "newRole", newRole.name(),
                        "changedBy", admin.getUsername()
                )
        );
    }
    @Transactional
    public void deactivateUser(UUID userId) {

        User admin = currentUserService.getCurrentUser();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (admin.getId().equals(userId)) {
            throw new IllegalArgumentException("Admin cannot deactivate himself");
        }

        if (!user.isActive()) {
            throw new UserAlreadyInActiveException();
        }

        user.setActive(false);

        auditLogService.log(
                admin,
                AuditAction.USER_DEACTIVATED,
                "USER",
                userId,
                Map.of(
                        "targetUserId", userId,
                        "targetUsername", user.getUsername(),
                        "deactivatedBy", admin.getUsername(),
                        "wasActive", true
                )
        );
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(MapUser::mapUser)
                .toList();
    }
}