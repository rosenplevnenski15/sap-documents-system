package com.sap.documentssystem.service;

import com.sap.documentssystem.model.AuditLog;
import com.sap.documentssystem.model.User;
import com.sap.documentssystem.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(User user, String action, String entityType, UUID entityId) {

        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID())
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }
}