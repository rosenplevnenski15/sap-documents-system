package com.sap.documentssystem.service;

import com.sap.documentssystem.dto.CommentResponse;
import com.sap.documentssystem.dto.CreateCommentRequest;
import com.sap.documentssystem.exceptions.InvalidVersionStateException;
import com.sap.documentssystem.exceptions.VersionNotFoundException;
import com.sap.documentssystem.mapper.CommentMapper;
import com.sap.documentssystem.entity.Comment;
import com.sap.documentssystem.entity.DocumentVersion;
import com.sap.documentssystem.entity.User;
import com.sap.documentssystem.repository.CommentRepository;
import com.sap.documentssystem.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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
    public CommentResponse addComment(UUID versionId, CreateCommentRequest request) {

        String content = request.getContent().trim();
        User user = currentUserService.getCurrentUser();
        authorizationService.canComment(user);

        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(VersionNotFoundException::new);


        if (version.getStatus() != com.sap.documentssystem.entity.VersionStatus.IN_REVIEW) {
            throw new InvalidVersionStateException("Comments allowed only for IN_REVIEW versions");
        }

        Comment comment = Comment.builder()
                .documentVersion(version)
                .user(user)
                .content(content)
                .build();

        Comment saved = commentRepository.save(comment);

        auditLogService.log(
                user,
                com.sap.documentssystem.entity.AuditAction.ADD_COMMENT,
                "COMMENT",
                saved.getId(),
                Map.of(
                        "versionId", versionId,
                        "contentLength", content.length()
                )
        );

        Comment full = commentRepository.findByIdWithUser(saved.getId())
                .orElseThrow();

        return CommentMapper.toResponse(full);
    }

    public List<CommentResponse> getComments(UUID versionId) {

        User user = currentUserService.getCurrentUser();
        authorizationService.canRead(user);

        return commentRepository
                .findAllWithUser(versionId)
                .stream()
                .map(CommentMapper::toResponse)
                .toList();
    }
}