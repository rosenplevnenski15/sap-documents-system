package service;

import com.sap.documentssystem.entity.*;
import com.sap.documentssystem.exceptions.*;
import com.sap.documentssystem.repository.DocumentRepository;
import com.sap.documentssystem.repository.DocumentVersionRepository;
import com.sap.documentssystem.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class DocumentVersionServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentVersionRepository versionRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private CurrentUserService currentUserService;
    @Mock private AuthorizationService authorizationService;
    @Mock private FileService fileService;
    @Mock private S3Service s3Service;

    @InjectMocks
    private DocumentVersionService service;

    // ---------------- HELPERS ----------------

    private User user(Role role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername("john");
        u.setRole(role);
        return u;
    }

    private User admin() {
        return user(Role.ADMIN);
    }

    private User author() {
        return user(Role.AUTHOR);
    }

    private Document document(User user) {
        Document d = new Document();
        d.setId(UUID.randomUUID());
        d.setCreatedBy(user);
        return d;
    }

    private DocumentVersion version(Document doc, VersionStatus status) {
        DocumentVersion v = new DocumentVersion();
        v.setId(UUID.randomUUID());
        v.setDocument(doc);
        v.setStatus(status);
        v.setVersionNumber(1);
        v.setFileName("file.txt");
        v.setS3Url("url");
        v.setCreatedBy(doc.getCreatedBy());
        return v;
    }

    private MultipartFile file() {
        return new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "hello".getBytes()
        );
    }

    private DocumentVersion version() {
        User u = user(Role.ADMIN);
        return version(document(u), VersionStatus.DRAFT);
    }

    // ---------------- CREATE VERSION ----------------

    @Test
    void shouldCreateVersionSuccessfully() {

        UUID docId = UUID.randomUUID();
        User user = user(Role.AUTHOR);
        Document doc = document(user);
        MultipartFile file = mock(MultipartFile.class);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        doNothing().when(authorizationService).canCreateVersion(user);

        when(fileService.upload(file)).thenReturn("url");
        when(file.getOriginalFilename()).thenReturn("file.txt");

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(versionRepository.findTopByDocument_IdOrderByVersionNumberDesc(docId))
                .thenReturn(Optional.empty());

        DocumentVersion saved = version(doc, VersionStatus.DRAFT);

        when(versionRepository.save(any())).thenReturn(saved);
        when(versionRepository.findFullById(saved.getId()))
                .thenReturn(Optional.of(saved));

        var result = service.createVersion(docId, file);

        assertThat(result).isNotNull();
        verify(fileService).upload(file);
    }

    @Test
    void shouldDeleteFileWhenCreateVersionFails() {

        UUID docId = UUID.randomUUID();
        User user = user(Role.AUTHOR);
        MultipartFile file = mock(MultipartFile.class);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        doNothing().when(authorizationService).canCreateVersion(user);

        when(fileService.upload(file)).thenReturn("url");
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.createVersion(docId, file)
        ).isInstanceOf(DocumentNotFoundException.class);

        verify(fileService).delete("url");
    }

    // ---------------- SUBMIT ----------------

    @Test
    void shouldSubmitForReview() {

        User user = user(Role.AUTHOR);
        DocumentVersion v = version(document(user), VersionStatus.DRAFT);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        doNothing().when(authorizationService).canSubmitForReview(user);

        when(versionRepository.findFullById(v.getId())).thenReturn(Optional.of(v));
        when(versionRepository.save(v)).thenReturn(v);

        service.submitForReview(v.getId());

        assertThat(v.getStatus()).isEqualTo(VersionStatus.IN_REVIEW);
    }

    @Test
    void shouldFailSubmitWhenNotDraft() {

        User user = user(Role.AUTHOR);
        DocumentVersion v = version(document(user), VersionStatus.APPROVED);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        doNothing().when(authorizationService).canSubmitForReview(user);

        when(versionRepository.findFullById(v.getId())).thenReturn(Optional.of(v));

        assertThatThrownBy(() ->
                service.submitForReview(v.getId())
        ).isInstanceOf(InvalidVersionStateException.class);
    }

    // ---------------- APPROVE ----------------

    @Test
    void shouldApproveVersion() {

        User reviewer = user(Role.REVIEWER);
        Document doc = document(reviewer);
        DocumentVersion v = version(doc, VersionStatus.IN_REVIEW);

        when(currentUserService.getCurrentUser()).thenReturn(reviewer);
        doNothing().when(authorizationService).canApprove(reviewer);

        when(versionRepository.findFullById(v.getId())).thenReturn(Optional.of(v));
        when(versionRepository.saveAndFlush(v)).thenReturn(v);

        service.approveVersion(v.getId());

        assertThat(v.getStatus()).isEqualTo(VersionStatus.APPROVED);
        assertThat(v.isActive()).isTrue();
    }

    // ---------------- REJECT ----------------

    @Test
    void shouldRejectVersion() {

        User reviewer = user(Role.REVIEWER);
        DocumentVersion v = version(document(reviewer), VersionStatus.IN_REVIEW);

        when(currentUserService.getCurrentUser()).thenReturn(reviewer);
        doNothing().when(authorizationService).canReject(reviewer);

        when(versionRepository.findFullById(v.getId())).thenReturn(Optional.of(v));
        when(versionRepository.save(v)).thenReturn(v);

        service.rejectVersion(v.getId());

        assertThat(v.getStatus()).isEqualTo(VersionStatus.REJECTED);
    }

    // ---------------- GET VERSIONS ----------------

    @Test
    void shouldReturnApprovedForReader() {

        User user = user(Role.READER);
        UUID docId = UUID.randomUUID();
        Document doc = document(user);

        DocumentVersion v = version(doc, VersionStatus.APPROVED);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        when(versionRepository.findByDocument_IdAndIsActiveTrueAndStatus(docId, VersionStatus.APPROVED))
                .thenReturn(List.of(v));

        var result = service.getVersions(docId);

        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
    }

    // ---------------- GET ACTIVE ----------------

    @Test
    void shouldGetActiveVersion() {

        User user = user(Role.ADMIN);
        DocumentVersion v = version(document(user), VersionStatus.APPROVED);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        doNothing().when(authorizationService).canRead(user);

        when(versionRepository.findActiveFullByDocumentId(any()))
                .thenReturn(Optional.of(v));

        var result = service.getActiveVersion(UUID.randomUUID());

        assertThat(result).isNotNull();
    }

    @Test
    void shouldThrowWhenActiveVersionNotApproved() {
        UUID docId = UUID.randomUUID();

        DocumentVersion version = version();
        version.setStatus(VersionStatus.DRAFT);

        when(currentUserService.getCurrentUser()).thenReturn(admin());
        when(versionRepository.findActiveFullByDocumentId(docId))
                .thenReturn(Optional.of(version));

        assertThatThrownBy(() ->
                service.getActiveVersion(docId)
        ).isInstanceOf(InvalidVersionStateException.class);
    }

    // ---------------- UPDATE DRAFT ----------------

    @Test
    void shouldUpdateDraftFile() {

        User user = user(Role.AUTHOR);
        Document doc = document(user);
        DocumentVersion v = version(doc, VersionStatus.DRAFT);

        MultipartFile file = mock(MultipartFile.class);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        doNothing().when(authorizationService).canEditDraft(user);

        when(versionRepository.findById(v.getId())).thenReturn(Optional.of(v));

        when(fileService.upload(file)).thenReturn("newUrl");
        when(file.getOriginalFilename()).thenReturn("new.txt");

        when(versionRepository.findFullById(v.getId())).thenReturn(Optional.of(v));

        service.updateDraftFile(v.getId(), file);

        verify(fileService).delete("url");
    }

    @Test
    void shouldThrowWhenUpdateNotDraft() {
        UUID versionId = UUID.randomUUID();

        DocumentVersion version = version();
        version.setStatus(VersionStatus.APPROVED);

        when(currentUserService.getCurrentUser()).thenReturn(admin());
        when(versionRepository.findById(versionId))
                .thenReturn(Optional.of(version));

        assertThatThrownBy(() ->
                service.updateDraftFile(versionId, file())
        ).isInstanceOf(InvalidVersionStateException.class);
    }

    @Test
    void shouldThrowWhenAuthorNotOwner() {
        UUID versionId = UUID.randomUUID();

        User author = author();
        author.setId(UUID.randomUUID());

        DocumentVersion version = version();
        version.setCreatedBy(new User()); // different user
        version.getCreatedBy().setId(UUID.randomUUID());

        when(currentUserService.getCurrentUser()).thenReturn(author);
        when(versionRepository.findById(versionId))
                .thenReturn(Optional.of(version));

        assertThatThrownBy(() ->
                service.updateDraftFile(versionId, file())
        ).isInstanceOf(AccessDeniedException.class);
    }

    // ---------------- COMPARE ----------------

    @Test
    void shouldCompareVersions() {

        User user = user(Role.ADMIN);
        Document doc = document(user);

        DocumentVersion v1 = version(doc, VersionStatus.APPROVED);
        DocumentVersion v2 = version(doc, VersionStatus.APPROVED);

        v1.setFileName("a.txt");
        v2.setFileName("b.txt");

        when(versionRepository.findFullById(v1.getId())).thenReturn(Optional.of(v1));
        when(versionRepository.findFullById(v2.getId())).thenReturn(Optional.of(v2));

        when(currentUserService.getCurrentUser()).thenReturn(user);
        doNothing().when(authorizationService).canCompare(user);

        when(s3Service.downloadFileAsText("url")).thenReturn("text");

        var result = service.compare(v1.getId(), v2.getId());

        assertThat(result).isNotNull();
    }

    @Test
    void shouldThrowWhenComparingSameVersion() {

        UUID id = UUID.randomUUID();

        User admin = admin();

        DocumentVersion version = version(document(admin), VersionStatus.APPROVED);
        version.setId(id);
        version.setFileName("file.txt");

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        doNothing().when(authorizationService).canCompare(admin);

        when(versionRepository.findFullById(id))
                .thenReturn(Optional.of(version));

        assertThatThrownBy(() ->
                service.compare(id, id)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenFilesNotTxt() {
        UUID v1 = UUID.randomUUID();
        UUID v2 = UUID.randomUUID();

        DocumentVersion version1 = version();
        version1.setFileName("file.pdf");

        DocumentVersion version2 = version();
        version2.setFileName("file.txt");

        when(versionRepository.findFullById(v1)).thenReturn(Optional.of(version1));
        when(versionRepository.findFullById(v2)).thenReturn(Optional.of(version2));

        when(currentUserService.getCurrentUser()).thenReturn(admin());

        assertThatThrownBy(() ->
                service.compare(v1, v2)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenNoActiveVersion() {
        UUID docId = UUID.randomUUID();

        when(currentUserService.getCurrentUser()).thenReturn(admin());
        when(versionRepository.findActiveFullByDocumentId(docId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.compareLatest(docId)
        ).isInstanceOf(VersionNotFoundException.class);
    }

    @Test
    void shouldThrowWhenNoInReviewVersion() {
        UUID docId = UUID.randomUUID();

        DocumentVersion active = version();
        active.setFileName("file.txt");

        when(currentUserService.getCurrentUser()).thenReturn(admin());
        when(versionRepository.findActiveFullByDocumentId(docId))
                .thenReturn(Optional.of(active));

        when(versionRepository.findInReviewFull(docId, VersionStatus.IN_REVIEW))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                service.compareLatest(docId)
        ).isInstanceOf(VersionNotFoundException.class);
    }

    // ---------------- EXPORT PDF ----------------

    @Test
    void shouldExportPdf() {

        User user = user(Role.ADMIN);
        DocumentVersion v = version(document(user), VersionStatus.APPROVED);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        doNothing().when(authorizationService).canRead(user);

        when(versionRepository.findFullById(v.getId()))
                .thenReturn(Optional.of(v));

        when(s3Service.downloadFileAsText("url"))
                .thenReturn("hello");

        byte[] result = service.exportToPdf(v.getId());

        assertThat(result).isNotEmpty();
    }

    @Test
    void shouldThrowExportWhenNotApproved() {
        UUID id = UUID.randomUUID();

        DocumentVersion version = version();
        version.setStatus(VersionStatus.DRAFT);

        when(currentUserService.getCurrentUser()).thenReturn(admin());
        when(versionRepository.findFullById(id))
                .thenReturn(Optional.of(version));

        assertThatThrownBy(() ->
                service.exportToPdf(id)
        ).isInstanceOf(InvalidVersionStateException.class);
    }

    @Test
    void shouldThrowExportWhenNotTxt() {
        UUID id = UUID.randomUUID();

        DocumentVersion version = version();
        version.setStatus(VersionStatus.APPROVED);
        version.setFileName("file.pdf");

        when(currentUserService.getCurrentUser()).thenReturn(admin());
        when(versionRepository.findFullById(id))
                .thenReturn(Optional.of(version));

        assertThatThrownBy(() ->
                service.exportToPdf(id)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenPdfFails() {
        UUID id = UUID.randomUUID();

        DocumentVersion version = version();
        version.setStatus(VersionStatus.APPROVED);
        version.setFileName("file.txt");

        when(currentUserService.getCurrentUser()).thenReturn(admin());
        when(versionRepository.findFullById(id))
                .thenReturn(Optional.of(version));

        when(s3Service.downloadFileAsText(any()))
                .thenThrow(new RuntimeException());

        assertThatThrownBy(() ->
                service.exportToPdf(id)
        ).isInstanceOf(FileStorageException.class);
    }
}