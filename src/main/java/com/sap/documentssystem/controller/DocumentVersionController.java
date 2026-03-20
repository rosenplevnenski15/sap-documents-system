package com.sap.documentssystem.controller;

import com.sap.documentssystem.dto.CreateVersionRequest;
import com.sap.documentssystem.dto.VersionResponse;
import com.sap.documentssystem.service.DocumentVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/versions")
@RequiredArgsConstructor
public class DocumentVersionController {

    private final DocumentVersionService versionService;

    @PostMapping("/documents/{documentId}/versions")
    public ResponseEntity<VersionResponse> createVersion(
            @PathVariable UUID documentId,
            @RequestParam MultipartFile file
    ) {
        VersionResponse response = versionService.createVersion(documentId, file);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/versions/{versionId}/submit")
    public ResponseEntity<VersionResponse> submitForReview(@PathVariable UUID versionId) {
        return ResponseEntity.ok(versionService.submitForReview(versionId));
    }

    @PostMapping("/versions/{versionId}/approve")
    public ResponseEntity<VersionResponse> approveVersion(@PathVariable UUID versionId) {
        return ResponseEntity.ok(versionService.approveVersion(versionId));
    }

    @PostMapping("/versions/{versionId}/reject")
    public ResponseEntity<VersionResponse> rejectVersion(@PathVariable UUID versionId) {
        return ResponseEntity.ok(versionService.rejectVersion(versionId));
    }

    @GetMapping("/documents/{documentId}/versions")
    public ResponseEntity<List<VersionResponse>> getVersions(@PathVariable UUID documentId) {
        return ResponseEntity.ok(versionService.getVersions(documentId));
    }

    @GetMapping("/documents/{documentId}/active")
    public ResponseEntity<VersionResponse> getActiveVersion(@PathVariable UUID documentId) {
        return ResponseEntity.ok(versionService.getActiveVersion(documentId));
    }

    @PutMapping("/versions/{versionId}")
    public ResponseEntity<VersionResponse> updateDraft(
            @PathVariable UUID versionId,
            @Valid @RequestBody CreateVersionRequest request
    ) {
        VersionResponse response = versionService.updateDraft(
                versionId,
                request.getFileName(),
                request.getS3Url()
        );

        return ResponseEntity.ok(response);
    }
}