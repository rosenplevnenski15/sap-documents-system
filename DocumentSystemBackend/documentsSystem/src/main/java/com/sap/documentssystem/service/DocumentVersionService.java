package com.sap.documentssystem.service;

import com.sap.documentssystem.dto.CreateVersionRequest;
import com.sap.documentssystem.dto.VersionResponse;
import com.sap.documentssystem.exceptions.DocumentNotFoundException;
import com.sap.documentssystem.exceptions.InvalidVersionStateException;
import com.sap.documentssystem.exceptions.VersionNotFoundException;
import com.sap.documentssystem.mapper.VersionMapper;
import com.sap.documentssystem.model.*;
import com.sap.documentssystem.repository.DocumentRepository;
import com.sap.documentssystem.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentVersionService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;
    private final FileService fileService;

    @Transactional
    public VersionResponse createVersion(UUID documentId, MultipartFile file) {

        User user = currentUserService.getCurrentUser();
        authorizationService.canCreateVersion(user);

        String fileUrl = null;

        try {
            // 1. upload to S3
            fileUrl = fileService.upload(file);

            Document document = documentRepository.findById(documentId)
                    .orElseThrow(DocumentNotFoundException::new);

            Integer lastVersion = versionRepository
                    .findTopByDocument_IdOrderByVersionNumberDesc(documentId)
                    .map(DocumentVersion::getVersionNumber)
                    .orElse(0);

            DocumentVersion version = DocumentVersion.builder()
                    .document(document)
                    .versionNumber(lastVersion + 1)
                    .fileName(file.getOriginalFilename())
                    .s3Url(fileUrl)
                    .status(VersionStatus.DRAFT)
                    .createdBy(user)
                    .build();

            DocumentVersion saved = versionRepository.save(version);

            auditLogService.log(
                    user,
                    AuditAction.CREATE_VERSION,
                    "VERSION",
                    saved.getId()
            );

            return VersionMapper.toResponse(saved);

        } catch (Exception ex) {


            if (fileUrl != null) {
                fileService.delete(fileUrl);
            }

            throw ex;
        }
    }

    @Transactional
    public VersionResponse submitForReview(UUID versionId) {
        User user = currentUserService.getCurrentUser();
        authorizationService.canSubmitForReview(user);

        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(VersionNotFoundException::new);

        if (version.getStatus() != VersionStatus.DRAFT) {
            throw new InvalidVersionStateException("Only DRAFT can be submitted");
        }

        version.setStatus(VersionStatus.IN_REVIEW);

        DocumentVersion saved = versionRepository.save(version);

        auditLogService.log(
                user,
                AuditAction.SUBMIT_FOR_REVIEW,
                "VERSION",
                saved.getId()
        );

        return VersionMapper.toResponse(saved);
    }

    @Transactional
    public VersionResponse approveVersion(UUID versionId) {
        User reviewer = currentUserService.getCurrentUser();
        authorizationService.canApprove(reviewer);

        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(VersionNotFoundException::new);

        if (version.getStatus() != VersionStatus.IN_REVIEW) {
            throw new InvalidVersionStateException("Only IN_REVIEW versions can be approved");
        }

        UUID documentId = version.getDocument().getId();

        versionRepository.deactivateOtherActiveVersions(documentId, versionId);
        versionRepository.flush();

        version.setStatus(VersionStatus.APPROVED);
        version.setApprovedAt(LocalDateTime.now());
        version.setApprovedBy(reviewer);
        version.setActive(true);

        DocumentVersion saved = versionRepository.saveAndFlush(version);

        auditLogService.log(
                reviewer,
                AuditAction.APPROVE_VERSION,
                "VERSION",
                saved.getId()
        );

        return VersionMapper.toResponse(saved);
    }

    @Transactional
    public VersionResponse rejectVersion(UUID versionId) {
        User reviewer = currentUserService.getCurrentUser();
        authorizationService.canReject(reviewer);

        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(VersionNotFoundException::new);

        if (version.getStatus() != VersionStatus.IN_REVIEW) {
            throw new InvalidVersionStateException("Only IN_REVIEW can be rejected");
        }

        version.setStatus(VersionStatus.REJECTED);

        DocumentVersion saved = versionRepository.save(version);

        auditLogService.log(
                reviewer,
                AuditAction.REJECT_VERSION,
                "VERSION",
                saved.getId()
        );

        return VersionMapper.toResponse(saved);
    }

    public List<VersionResponse> getVersions(UUID documentId) {

        User user = currentUserService.getCurrentUser();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(DocumentNotFoundException::new);


        if (user.getRole() == Role.AUTHOR &&
                !document.getCreatedBy().getId().equals(user.getId())) {

            throw new RuntimeException("Authors can only view their own documents");
        }


        if (user.getRole() == Role.READER) {
            return versionRepository
                    .findByDocument_IdAndIsActiveTrueAndStatus(documentId, VersionStatus.APPROVED)
                    .stream()
                    .map(VersionMapper::toResponse)
                    .toList();
        }


        if (user.getRole() == Role.REVIEWER) {
            return versionRepository
                    .findByDocument_IdAndStatus(documentId, VersionStatus.IN_REVIEW)
                    .stream()
                    .map(VersionMapper::toResponse)
                    .toList();
        }


        return versionRepository.findByDocument_IdOrderByVersionNumberDesc(documentId)
                .stream()
                .map(VersionMapper::toResponse)
                .toList();
    }




    public VersionResponse getActiveVersion(UUID documentId) {
        User user = currentUserService.getCurrentUser();
        authorizationService.canRead(user);

        DocumentVersion version = versionRepository
                .findByDocument_IdAndIsActiveTrue(documentId)
                .orElseThrow(() -> new VersionNotFoundException("Active version not found"));

        if (version.getStatus() != VersionStatus.APPROVED) {
            throw new InvalidVersionStateException("Only approved versions can be read");
        }

        return VersionMapper.toResponse(version);
    }

    @Transactional
    public VersionResponse updateDraft(UUID versionId, String fileName, String s3Url) {

        User user = currentUserService.getCurrentUser();


        authorizationService.canEditDraft(user);

        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(VersionNotFoundException::new);


        if (version.getStatus() != VersionStatus.DRAFT) {
            throw new InvalidVersionStateException("Only DRAFT versions can be edited");
        }


        if (user.getRole() == Role.AUTHOR &&
                !version.getCreatedBy().getId().equals(user.getId())) {

            throw new RuntimeException("Authors can edit only their own drafts");
        }

        version.setFileName(fileName);
        version.setS3Url(s3Url);

        DocumentVersion saved = versionRepository.save(version);

        auditLogService.log(
                user,
                AuditAction.EDIT_DRAFT,
                "VERSION",
                saved.getId()
        );

        return VersionMapper.toResponse(saved);
    }


}