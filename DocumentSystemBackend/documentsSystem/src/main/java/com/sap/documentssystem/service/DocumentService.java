package com.sap.documentssystem.service;

import com.sap.documentssystem.dto.DocumentResponse;
import com.sap.documentssystem.exceptions.FileStorageException;
import com.sap.documentssystem.mapper.DocumentMapper;
import com.sap.documentssystem.entity.*;
import com.sap.documentssystem.repository.DocumentRepository;
import com.sap.documentssystem.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.time.LocalDateTime;
import java.util.Map;

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



}