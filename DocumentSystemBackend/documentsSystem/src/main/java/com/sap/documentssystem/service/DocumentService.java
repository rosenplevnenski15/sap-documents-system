package com.sap.documentssystem.service;

import com.sap.documentssystem.dto.DocumentResponse;
import com.sap.documentssystem.exceptions.FileStorageException;
import com.sap.documentssystem.mapper.DocumentMapper;
import com.sap.documentssystem.model.*;
import com.sap.documentssystem.repository.DocumentRepository;
import com.sap.documentssystem.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final AuditLogService auditService;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;
    private final DocumentVersionRepository versionRepository;
    private final FileService fileService;

    @Transactional
    public DocumentResponse createDocument(String title, MultipartFile file) {

        User user = currentUserService.getCurrentUser();
        authorizationService.canCreateDocument(user);

        if (file == null || file.isEmpty()) {
            throw new FileStorageException("File is required");
        }

        String fileUrl = null;

        try {
            // 1. Upload file to S3

            fileUrl = fileService.upload(file);


            // 2. Create document
            Document document = Document.builder()
                    .title(title)
                    .createdBy(user)
                    .createdAt(LocalDateTime.now())
                    .build();

            documentRepository.save(document);

            // 3. Create VERSION 1 (DRAFT)
            DocumentVersion version = DocumentVersion.builder()
                    .document(document)
                    .versionNumber(1)
                    .fileName(file.getOriginalFilename())
                    .s3Url(fileUrl)
                    .status(VersionStatus.DRAFT)
                    .isActive(false)
                    .createdBy(user)
                    .build();

            versionRepository.save(version);

            // 4. Audit
            auditService.log(
                    user,
                    AuditAction.CREATE_DOCUMENT,
                    "DOCUMENT",
                    document.getId()
            );

            auditService.log(
                    user,
                    AuditAction.CREATE_VERSION,
                    "VERSION",
                    version.getId()
            );

            return DocumentMapper.toResponse(document);

        } catch (Exception ex) {

            // rollback S3 ако DB fail
            if (fileUrl != null) {
                fileService.delete(fileUrl);
            }

            throw ex;
        }
    }
}