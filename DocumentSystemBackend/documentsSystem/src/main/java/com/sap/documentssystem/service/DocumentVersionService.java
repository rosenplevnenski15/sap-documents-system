package com.sap.documentssystem.service;

import com.sap.documentssystem.dto.VersionResponse;
import com.sap.documentssystem.exceptions.DocumentNotFoundException;
import com.sap.documentssystem.exceptions.InvalidVersionStateException;
import com.sap.documentssystem.exceptions.VersionNotFoundException;
import com.sap.documentssystem.mapper.VersionMapper;
import com.sap.documentssystem.model.Document;
import com.sap.documentssystem.model.DocumentVersion;
import com.sap.documentssystem.model.User;
import com.sap.documentssystem.model.VersionStatus;
import com.sap.documentssystem.repository.DocumentRepository;
import com.sap.documentssystem.repository.DocumentVersionRepository;
import com.sap.documentssystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentVersionService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional
    public VersionResponse createVersion(UUID documentId, String fileName, String s3Url) {
        User createdBy = getCurrentUser();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(DocumentNotFoundException::new);

        Integer lastVersion = versionRepository
                .findTopByDocument_IdOrderByVersionNumberDesc(documentId)
                .map(DocumentVersion::getVersionNumber)
                .orElse(0);

        DocumentVersion version = DocumentVersion.builder()
                .id(UUID.randomUUID())
                .document(document)
                .versionNumber(lastVersion + 1)
                .fileName(fileName)
                .s3Url(s3Url)
                .status(VersionStatus.DRAFT)
                .isActive(false)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();

        DocumentVersion saved = versionRepository.save(version);

        auditLogService.log(
                createdBy,
                "CREATE_VERSION",
                "DOCUMENT_VERSION",
                saved.getId()
        );

        return VersionMapper.toResponse(saved);
    }

    @Transactional
    public VersionResponse submitForReview(UUID versionId) {
        User user = getCurrentUser();

        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(VersionNotFoundException::new);

        if (version.getStatus() != VersionStatus.DRAFT) {
            throw new InvalidVersionStateException("Only DRAFT can be submitted");
        }

        version.setStatus(VersionStatus.IN_REVIEW);

        DocumentVersion saved = versionRepository.save(version);

        auditLogService.log(
                user,
                "SUBMIT_VERSION",
                "DOCUMENT_VERSION",
                saved.getId()
        );

        return VersionMapper.toResponse(saved);
    }

    @Transactional
    public VersionResponse approveVersion(UUID versionId) {
        User reviewer = getCurrentUser();

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
        version.setIsActive(true);

        DocumentVersion saved = versionRepository.saveAndFlush(version);

        auditLogService.log(
                reviewer,
                "APPROVE_VERSION",
                "DOCUMENT_VERSION",
                saved.getId()
        );

        return VersionMapper.toResponse(saved);
    }

    @Transactional
    public VersionResponse rejectVersion(UUID versionId) {
        User reviewer = getCurrentUser();

        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(VersionNotFoundException::new);

        if (version.getStatus() != VersionStatus.IN_REVIEW) {
            throw new InvalidVersionStateException("Only IN_REVIEW can be rejected");
        }

        version.setStatus(VersionStatus.REJECTED);

        DocumentVersion saved = versionRepository.save(version);

        auditLogService.log(
                reviewer,
                "REJECT_VERSION",
                "DOCUMENT_VERSION",
                saved.getId()
        );

        return VersionMapper.toResponse(saved);
    }

    public List<VersionResponse> getVersions(UUID documentId) {
        return versionRepository.findByDocument_IdOrderByVersionNumberDesc(documentId)
                .stream()
                .map(VersionMapper::toResponse)
                .toList();
    }
}