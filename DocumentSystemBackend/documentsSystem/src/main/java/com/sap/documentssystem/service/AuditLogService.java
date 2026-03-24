package com.sap.documentssystem.service;

import com.sap.documentssystem.model.AuditAction;
import com.sap.documentssystem.model.AuditLog;
import com.sap.documentssystem.model.User;
import com.sap.documentssystem.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
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
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }


   public void  log(User user, AuditAction action, String entityType, UUID entityId)
   {
       AuditLog log = AuditLog.builder()
               .user(user)
               .action(action)
               .entityType(entityType)
               .entityId(entityId)
               .details(new HashMap<>())
               .createdAt(LocalDateTime.now())
               .build();

       auditLogRepository.save(log);
   }
}