package com.sap.documentssystem.controller;

import com.sap.documentssystem.dto.CreateVersionRequest;
import com.sap.documentssystem.dto.VersionResponse;
import com.sap.documentssystem.service.DocumentVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentVersionController {

    private final DocumentVersionService versionService;

    @PostMapping("/documents/{documentId}/versions")
    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    public VersionResponse createVersion(
            @PathVariable UUID documentId,
            @Valid @RequestBody CreateVersionRequest request
    ) {

        return versionService.createVersion(
                documentId,
                request.getFileName(),
                request.getS3Url()
        );
    }

    @PostMapping("/versions/{versionId}/submit")
    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    public VersionResponse submitForReview(@PathVariable UUID versionId) {

        return versionService.submitForReview(versionId);
    }

    @PostMapping("/versions/{versionId}/approve")
    @PreAuthorize("hasAnyRole('REVIEWER','ADMIN')")
    public VersionResponse approveVersion(@PathVariable UUID versionId) {

        return versionService.approveVersion(versionId);
    }

    @PostMapping("/versions/{versionId}/reject")
    @PreAuthorize("hasAnyRole('REVIEWER','ADMIN')")
    public VersionResponse rejectVersion(@PathVariable UUID versionId) {

        return versionService.rejectVersion(versionId);
    }

    @GetMapping("/documents/{documentId}/versions")
    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    public List<VersionResponse> getVersions(@PathVariable UUID documentId) {

        return versionService.getVersions(documentId);
    }
}