package com.sap.documentssystem.service;

import com.sap.documentssystem.dto.DocumentResponse;
import com.sap.documentssystem.mapper.DocumentMapper;
import com.sap.documentssystem.model.AuditAction;
import com.sap.documentssystem.model.Document;
import com.sap.documentssystem.model.User;
import com.sap.documentssystem.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final AuditLogService auditService;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;

    @Transactional
    public DocumentResponse createDocument(String title) {

        User user = currentUserService.getCurrentUser();
        authorizationService.canCreateDocument(user);

        Document document = Document.builder()
                .title(title)
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .build();

        documentRepository.save(document);

        auditService.log(
                user,
                AuditAction.CREATE_DOCUMENT,
                "DOCUMENT",
                document.getId(),
                null
        );

        return DocumentMapper.toResponse(document);
    }
}