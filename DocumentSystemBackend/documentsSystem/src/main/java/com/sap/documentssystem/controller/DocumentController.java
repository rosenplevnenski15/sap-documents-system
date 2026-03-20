package com.sap.documentssystem.controller;

import com.sap.documentssystem.dto.CreateDocumentRequest;
import com.sap.documentssystem.dto.DocumentResponse;
import com.sap.documentssystem.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(
            @Valid @RequestBody CreateDocumentRequest request
    ) {
        DocumentResponse response = documentService.createDocument(request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}