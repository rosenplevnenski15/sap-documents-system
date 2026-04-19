package service;

import com.sap.documentssystem.dto.DocumentResponse;
import com.sap.documentssystem.entity.*;
import com.sap.documentssystem.exceptions.FileStorageException;
import com.sap.documentssystem.repository.DocumentRepository;
import com.sap.documentssystem.repository.DocumentVersionRepository;
import com.sap.documentssystem.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentVersionRepository versionRepository;
    @Mock private AuditLogService auditService;
    @Mock private CurrentUserService currentUserService;
    @Mock private AuthorizationService authorizationService;
    @Mock private FileService fileService;

    @InjectMocks
    private DocumentService documentService;

    // 🔧 helper
    private User user() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername("john");
        return u;
    }

    // ---------------- SUCCESS ----------------

    @Test
    void shouldCreateDocumentSuccessfully() {

        User user = user();

        MultipartFile file = mock(MultipartFile.class);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        doNothing().when(authorizationService).canCreateDocument(user);

        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("file.txt");

        when(fileService.upload(file)).thenReturn("url");

        // save document → трябва да му зададем ID
        when(documentRepository.save(any())).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });

        // save version → също ID
        when(versionRepository.save(any())).thenAnswer(invocation -> {
            DocumentVersion v = invocation.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        DocumentResponse response =
                documentService.createDocument("title", file);

        verify(authorizationService).canCreateDocument(user);

        verify(fileService).upload(file);

        verify(documentRepository).save(any());

        verify(versionRepository).save(any());

        verify(auditService, times(2)).log(
                eq(user),
                any(),
                anyString(),
                any(),
                anyMap()
        );

        assertThat(response).isNotNull();
    }

    // ---------------- FILE NULL ----------------

    @Test
    void shouldThrowWhenFileIsNull() {

        User user = user();

        when(currentUserService.getCurrentUser()).thenReturn(user);

        assertThatThrownBy(() ->
                documentService.createDocument("title", null)
        ).isInstanceOf(FileStorageException.class);
    }

    // ---------------- FILE EMPTY ----------------

    @Test
    void shouldThrowWhenFileIsEmpty() {

        User user = user();

        MultipartFile file = mock(MultipartFile.class);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() ->
                documentService.createDocument("title", file)
        ).isInstanceOf(FileStorageException.class);
    }

    // ---------------- UPLOAD FAIL → DELETE FILE ----------------

    @Test
    void shouldDeleteFileWhenExceptionOccurs() {

        User user = user();
        MultipartFile file = mock(MultipartFile.class);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        doNothing().when(authorizationService).canCreateDocument(user);

        when(file.isEmpty()).thenReturn(false);

        when(fileService.upload(file)).thenReturn("url");

        when(documentRepository.save(any()))
                .thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() ->
                documentService.createDocument("title", file)
        ).isInstanceOf(RuntimeException.class);

        verify(fileService).delete("url");
    }
}
