package com.sap.documentssystem.service;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.Paragraph;
import com.sap.documentssystem.dto.CreateVersionRequest;
import com.sap.documentssystem.dto.VersionResponse;
import com.sap.documentssystem.exceptions.DocumentNotFoundException;
import com.sap.documentssystem.exceptions.InvalidVersionStateException;
import com.sap.documentssystem.exceptions.VersionNotFoundException;
import com.sap.documentssystem.mapper.VersionMapper;
import com.sap.documentssystem.model.*;
import com.sap.documentssystem.repository.DocumentRepository;
import com.sap.documentssystem.repository.DocumentVersionRepository;
import com.sap.documentssystem.exceptions.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;



import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final S3Service s3Service;

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
    public VersionResponse updateDraftFile(UUID versionId, MultipartFile file)  {

        User user = currentUserService.getCurrentUser();
        authorizationService.canEditDraft(user);

        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new VersionNotFoundException());


        if (version.getStatus() != VersionStatus.DRAFT) {
            throw new InvalidVersionStateException("Only DRAFT can be edited");
        }


        if (user.getRole() == Role.AUTHOR &&
                !version.getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can edit only your own drafts");
        }

        String oldUrl = version.getS3Url();


        String newUrl = null;

        try {
            newUrl = fileService.upload(file);

            version.setS3Url(newUrl);
            version.setFileName(file.getOriginalFilename());

            fileService.delete(oldUrl);

        } catch (Exception ex) {

            if (newUrl != null) {
                fileService.delete(newUrl);
            }

            throw ex;
        }


        auditLogService.log(
                user,
                AuditAction.EDIT_DRAFT,
                "VERSION",
                versionId,
                Map.of(
                        "oldFile", oldUrl,
                        "newFile", newUrl
                )
        );

        return VersionMapper.toResponse(version);
    }
    public String compare(UUID v1, UUID v2) {


        DocumentVersion version1 = versionRepository.findById(v1)
                .orElseThrow(() -> new VersionNotFoundException("Version 1 not found"));

        DocumentVersion version2 = versionRepository.findById(v2)
                .orElseThrow(() -> new VersionNotFoundException("Version 2 not found"));


        User user = currentUserService.getCurrentUser();
        authorizationService.canCompare(user);

        if (user.getRole() == Role.AUTHOR &&
                !version1.getDocument().getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can compare only your documents");
        }

        if (user.getRole() == Role.READER) {
            throw new AccessDeniedException("Readers cannot compare versions");
        }


        if (v1.equals(v2)) {
            throw new IllegalArgumentException("Cannot compare the same version");
        }


        if (!version1.getFileName().endsWith(".txt") ||
                !version2.getFileName().endsWith(".txt")) {
            throw new IllegalArgumentException("Only TXT files can be compared");
        }


        String text1 = s3Service.downloadFileAsText(version1.getS3Url());
        String text2 = s3Service.downloadFileAsText(version2.getS3Url());

        List<String> original = List.of(text1.split("\n"));
        List<String> revised = List.of(text2.split("\n"));


        Patch<String> patch = DiffUtils.diff(original, revised);


        auditLogService.log(
                user,
                AuditAction.DOWNLOAD_DOCUMENT,
                "VERSION_COMPARE",
                version1.getId(),
                Map.of(
                        "version1", v1,
                        "version2", v2
                )
        );


        return patch.getDeltas().toString();
    }
    public String compareLatest(UUID documentId) {

        User user = currentUserService.getCurrentUser();


        if (user.getRole() == Role.READER) {
            throw new AccessDeniedException("Readers cannot compare versions");
        }


        DocumentVersion active = versionRepository
                .findByDocument_IdAndIsActiveTrue(documentId)
                .orElseThrow(() -> new VersionNotFoundException("Active version not found"));


        DocumentVersion inReview = versionRepository
                .findTopByDocument_IdAndStatusOrderByVersionNumberDesc(
                        documentId,
                        VersionStatus.IN_REVIEW
                )
                .orElseThrow(() -> new VersionNotFoundException("No version in review"));


        if (user.getRole() == Role.AUTHOR &&
                !active.getDocument().getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can compare only your documents");
        }


        if (!active.getFileName().endsWith(".txt") ||
                !inReview.getFileName().endsWith(".txt")) {
            throw new IllegalArgumentException("Only TXT files supported");
        }

        return compare(active.getId(), inReview.getId());
    }

    public byte[] exportToPdf(UUID versionId) {

        User user = currentUserService.getCurrentUser();
        authorizationService.canRead(user);

        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new VersionNotFoundException("Version not found"));


        if (version.getStatus() != VersionStatus.APPROVED) {
            throw new InvalidVersionStateException("Only APPROVED versions can be exported");
        }


        if (!version.getFileName().endsWith(".txt")) {
            throw new IllegalArgumentException("Only TXT files can be exported to PDF");
        }

        try {


            String text = s3Service.downloadFileAsText(version.getS3Url());


            ByteArrayOutputStream out = new ByteArrayOutputStream();

            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);

            document.add(new Paragraph(text));

            document.close();


            auditLogService.log(
                    user,
                    AuditAction.EXPORT_DOCUMENT,
                    "VERSION",
                    versionId
            );

            return out.toByteArray();

        } catch (Exception ex) {
            throw new RuntimeException("Failed to export PDF", ex);
        }
    }

}