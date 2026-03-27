package com.sap.documentssystem.controller;

import com.sap.documentssystem.dto.VersionResponse;
import com.sap.documentssystem.service.DocumentVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    public ResponseEntity<VersionResponse> createVersion(
            @PathVariable UUID documentId,
            @RequestParam MultipartFile file
    ) {
        VersionResponse response = versionService.createVersion(documentId, file);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{versionId}/submit")
    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    public ResponseEntity<VersionResponse> submitForReview(@PathVariable UUID versionId) {
        return ResponseEntity.ok(versionService.submitForReview(versionId));
    }

    @PostMapping("/{versionId}/approve")
    @PreAuthorize("hasAnyRole('AUTHOR','REVIEWER')")
    public ResponseEntity<VersionResponse> approveVersion(@PathVariable UUID versionId) {
        return ResponseEntity.ok(versionService.approveVersion(versionId));
    }

    @PostMapping("/{versionId}/reject")
    @PreAuthorize("hasAnyRole('AUTHOR','REVIEWER')")
    public ResponseEntity<VersionResponse> rejectVersion(@PathVariable UUID versionId) {
        return ResponseEntity.ok(versionService.rejectVersion(versionId));
    }

    @GetMapping("/{documentId}/versions")
    public ResponseEntity<List<VersionResponse>> getVersions(@PathVariable UUID documentId) {
        return ResponseEntity.ok(versionService.getVersions(documentId));
    }

    @GetMapping("/{documentId}/active")
    public ResponseEntity<VersionResponse> getActiveVersion(@PathVariable UUID documentId) {
        return ResponseEntity.ok(versionService.getActiveVersion(documentId));
    }

    @PutMapping("/{versionId}/file")
    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    public ResponseEntity<VersionResponse> updateDraftFile(
            @PathVariable UUID versionId,
            @RequestParam MultipartFile file
    ) {
        return ResponseEntity.ok(versionService.updateDraftFile(versionId, file));
    }

    @GetMapping("/{versionId}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable UUID versionId) {

        byte[] pdf = versionService.exportToPdf(versionId);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=document.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }


}