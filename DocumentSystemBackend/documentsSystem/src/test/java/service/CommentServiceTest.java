package service;

import com.sap.documentssystem.dto.CommentResponse;
import com.sap.documentssystem.dto.CreateCommentRequest;
import com.sap.documentssystem.entity.Comment;
import com.sap.documentssystem.entity.DocumentVersion;
import com.sap.documentssystem.entity.VersionStatus;
import com.sap.documentssystem.exceptions.InvalidVersionStateException;
import com.sap.documentssystem.exceptions.VersionNotFoundException;
import com.sap.documentssystem.repository.CommentRepository;
import com.sap.documentssystem.repository.DocumentVersionRepository;
import com.sap.documentssystem.service.AuditLogService;
import com.sap.documentssystem.service.AuthorizationService;
import com.sap.documentssystem.service.CommentService;
import com.sap.documentssystem.service.CurrentUserService;
import com.sap.documentssystem.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {
    @Mock
    private CommentRepository commentRepository;
    @Mock private DocumentVersionRepository versionRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private AuthorizationService authorizationService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private CommentService commentService;

    private User user() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername("john");
        return u;
    }

    private DocumentVersion version(VersionStatus status) {
        DocumentVersion v = new DocumentVersion();
        v.setId(UUID.randomUUID());
        v.setStatus(status);
        return v;
    }

    // --------------- addComment() ---------------

    @Test
    void shouldAddCommentSuccessfully() {

        UUID versionId = UUID.randomUUID();

        User user = user();
        DocumentVersion version = version(VersionStatus.IN_REVIEW);

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("  hello comment  ");

        when(currentUserService.getCurrentUser()).thenReturn(user);

        when(versionRepository.findById(versionId))
                .thenReturn(Optional.of(version));

        // реалистичен Comment с user (важно за mapper-а)
        Comment savedComment = new Comment();
        savedComment.setId(UUID.randomUUID());
        savedComment.setUser(user);

        when(commentRepository.save(any()))
                .thenReturn(savedComment);

        when(commentRepository.findByIdWithUser(any()))
                .thenReturn(Optional.of(savedComment));

        // WHEN
        CommentResponse response = commentService.addComment(versionId, request);

        // THEN - verify
        verify(authorizationService).canComment(user);

        verify(commentRepository).save(any());

        verify(auditLogService).log(
                eq(user),
                any(),
                eq("COMMENT"),
                any(),
                anyMap()
        );
    }

    // --------------- INVALID VERSION STATE ---------------

    @Test
    void shouldThrowWhenVersionNotInReview() {

        UUID versionId = UUID.randomUUID();

        User user = user();
        DocumentVersion version = version(VersionStatus.DRAFT);

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("text");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(versionRepository.findById(versionId))
                .thenReturn(Optional.of(version));

        doNothing().when(authorizationService).canComment(user);

        assertThatThrownBy(() ->
                commentService.addComment(versionId, request)
        ).isInstanceOf(InvalidVersionStateException.class);
    }

    // --------------- VERSION NOT FOUND ---------------

    @Test
    void shouldThrowWhenVersionNotFound() {

        UUID versionId = UUID.randomUUID();

        User user = user();

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(versionRepository.findById(versionId))
                .thenReturn(Optional.empty());

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("text");

        assertThatThrownBy(() ->
                commentService.addComment(versionId, request)
        ).isInstanceOf(VersionNotFoundException.class);
    }

    // --------------- getComments() ---------------

    @Test
    void shouldReturnComments() {

        UUID versionId = UUID.randomUUID();

        User user = user();

        when(currentUserService.getCurrentUser()).thenReturn(user);

        doNothing().when(authorizationService).canRead(user);

        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        comment.setUser(user); // 🔥 FIX

        when(commentRepository.findAllWithUser(versionId))
                .thenReturn(List.of(comment));

        List<CommentResponse> result = commentService.getComments(versionId);

        verify(commentRepository).findAllWithUser(versionId);
        verify(authorizationService).canRead(user);

        assertThat(result).isNotNull();
    }
}
