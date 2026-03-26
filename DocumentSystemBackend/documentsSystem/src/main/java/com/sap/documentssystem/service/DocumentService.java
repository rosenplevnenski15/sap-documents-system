package com.sap.documentssystem.service;

import com.sap.documentssystem.dto.DocumentResponse;
import com.sap.documentssystem.exceptions.FileStorageException;
import com.sap.documentssystem.mapper.DocumentMapper;
import com.sap.documentssystem.model.*;
import com.sap.documentssystem.repository.DocumentRepository;
import com.sap.documentssystem.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.time.LocalDateTime;
import java.util.Map;
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


            fileUrl = fileService.upload(file);



            Document document = Document.builder()
                    .title(title)
                    .createdBy(user)
                    .createdAt(LocalDateTime.now())
                    .build();

            documentRepository.save(document);


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


            auditService.log(
                    user,
                    AuditAction.CREATE_DOCUMENT,
                    "DOCUMENT",
                    document.getId(),
                    Map.of("title",document.getTitle())
            );

            auditService.log(
                    user,
                    AuditAction.CREATE_VERSION,
                    "VERSION",
                    version.getId(),
                    Map.of("versionNumber",version.getVersionNumber(),
                            "fileName",version.getFileName())
            );

            return DocumentMapper.toResponse(document);

        } catch (Exception ex) {


            if (fileUrl != null) {
                fileService.delete(fileUrl);
            }

            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocuments(String query, boolean createdByMe, Pageable pageable) {
        String normalizedQuery = query == null ? "" : query.trim();
        User currentUser = currentUserService.getCurrentUser();
        UUID userId = currentUser.getId();

        Page<Document> documents;
        if (createdByMe && !normalizedQuery.isEmpty()) {
            documents = documentRepository.searchByCreatorAndQuery(userId, normalizedQuery, pageable);
        } else if (createdByMe) {
            documents = documentRepository.findByCreatedBy_Id(userId, pageable);
        } else if (!normalizedQuery.isEmpty()) {
            documents = documentRepository.findByTitleContainingIgnoreCase(normalizedQuery, pageable);
        } else {
            documents = documentRepository.findAll(pageable);
        }

        return documents.map(DocumentMapper::toResponse);
    }

}