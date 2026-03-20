package com.sap.documentssystem.controller;

import com.sap.documentssystem.dto.CommentResponse;
import com.sap.documentssystem.dto.CreateCommentRequest;
import com.sap.documentssystem.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/versions")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{versionId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID versionId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        CommentResponse response = commentService.addComment(versionId, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{versionId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable UUID versionId
    ) {
        return ResponseEntity.ok(commentService.getComments(versionId));
    }
}