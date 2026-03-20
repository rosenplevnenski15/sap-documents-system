package com.sap.documentssystem.service;

import com.sap.documentssystem.dto.CommentResponse;
import com.sap.documentssystem.mapper.CommentMapper;
import com.sap.documentssystem.model.Comment;
import com.sap.documentssystem.model.DocumentVersion;
import com.sap.documentssystem.model.User;
import com.sap.documentssystem.repository.CommentRepository;
import com.sap.documentssystem.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final DocumentVersionRepository versionRepository;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;
    private final AuditLogService auditLogService;

    @Transactional
    public CommentResponse addComment(UUID versionId, String content) {

        User user = currentUserService.getCurrentUser();

        // 🔒 role check
        authorizationService.canComment(user);

        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        // 🔥 бизнес правило: може да се коментира само IN_REVIEW
        if (version.getStatus() != com.sap.documentssystem.model.VersionStatus.IN_REVIEW) {
            throw new RuntimeException("Comments allowed only for IN_REVIEW versions");
        }

        Comment comment = Comment.builder()
                .documentVersion(version)
                .user(user)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();

        Comment saved = commentRepository.save(comment);

        auditLogService.log(
                user,
                com.sap.documentssystem.model.AuditAction.ADD_COMMENT,
                "COMMENT",
                saved.getId()
        );

        return CommentMapper.toResponse(saved);
    }

    public List<CommentResponse> getComments(UUID versionId) {

        User user = currentUserService.getCurrentUser();
        authorizationService.canRead(user);

        return commentRepository
                .findByDocumentVersion_IdOrderByCreatedAtAsc(versionId)
                .stream()
                .map(CommentMapper::toResponse)
                .toList();
    }
}