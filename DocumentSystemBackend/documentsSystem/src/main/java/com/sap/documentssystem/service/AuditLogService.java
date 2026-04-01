package com.sap.documentssystem.service;

import com.sap.documentssystem.entity.AuditAction;
import com.sap.documentssystem.entity.AuditLog;
import com.sap.documentssystem.entity.User;
import com.sap.documentssystem.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(User user,
                    AuditAction action,
                    String entityType,
                    UUID entityId,
                    Map<String,Object> details) {

        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();

        auditLogRepository.save(log);
    }

}