package com.sap.documentssystem.controller;

import com.sap.documentssystem.dto.CreateDocumentRequest;
import com.sap.documentssystem.dto.DocumentResponse;
import com.sap.documentssystem.service.DocumentService;
import com.sap.documentssystem.service.DocumentVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentVersionService versionService;


    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentResponse> createDocument(
            @Valid @ModelAttribute CreateDocumentRequest request,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.createDocument(request.getTitle(), file));
    }

    @GetMapping("/{documentId}/compare")
    @PreAuthorize("hasAnyRole('AUTHOR','REVIEWER','ADMIN')")
    public ResponseEntity<String> compare(@PathVariable UUID documentId) {
        return ResponseEntity.ok(versionService.compareLatest(documentId));
    }

}