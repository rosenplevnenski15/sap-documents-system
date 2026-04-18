package controller;

import com.sap.documentssystem.controller.DocumentVersionController;
import com.sap.documentssystem.dto.VersionResponse;
import com.sap.documentssystem.service.DocumentVersionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentVersionController.class)
class DocumentVersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentVersionService versionService;

    // ---------------- CREATE VERSION ----------------

    @Test
    @WithMockUser(roles = {"AUTHOR"})
    void createVersion_shouldReturn201() throws Exception {

        UUID documentId = UUID.randomUUID();

        MockMultipartFile file =
                new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        VersionResponse response = VersionResponse.builder()
                .build();

        Mockito.when(versionService.createVersion(eq(documentId), any()))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/versions/documents/{documentId}/versions", documentId)
                        .file(file))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void createVersion_shouldReturn403() throws Exception {

        UUID documentId = UUID.randomUUID();

        MockMultipartFile file =
                new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/versions/documents/{documentId}/versions", documentId)
                        .file(file))
                .andExpect(status().isForbidden());
    }

    // ---------------- SUBMIT ----------------

    @Test
    @WithMockUser(roles = {"AUTHOR"})
    void submitForReview_shouldReturn200() throws Exception {

        UUID versionId = UUID.randomUUID();

        VersionResponse response = VersionResponse.builder().build();

        Mockito.when(versionService.submitForReview(versionId))
                .thenReturn(response);

        mockMvc.perform(post("/api/versions/{versionId}/submit", versionId))
                .andExpect(status().isOk());
    }

    // ---------------- APPROVE ----------------

    @Test
    @WithMockUser(roles = {"REVIEWER"})
    void approveVersion_shouldReturn200() throws Exception {

        UUID versionId = UUID.randomUUID();

        VersionResponse response = VersionResponse.builder().build();

        Mockito.when(versionService.approveVersion(versionId))
                .thenReturn(response);

        mockMvc.perform(post("/api/versions/{versionId}/approve", versionId))
                .andExpect(status().isOk());
    }

    // ---------------- REJECT ----------------

    @Test
    @WithMockUser(roles = {"REVIEWER"})
    void rejectVersion_shouldReturn200() throws Exception {

        UUID versionId = UUID.randomUUID();

        VersionResponse response = VersionResponse.builder().build();

        Mockito.when(versionService.rejectVersion(versionId))
                .thenReturn(response);

        mockMvc.perform(post("/api/versions/{versionId}/reject", versionId))
                .andExpect(status().isOk());
    }

    // ---------------- GET VERSIONS ----------------

    @Test
    @WithMockUser
    void getVersions_shouldReturn200() throws Exception {

        UUID documentId = UUID.randomUUID();

        Mockito.when(versionService.getVersions(documentId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/versions/{documentId}/versions", documentId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    // ---------------- ACTIVE VERSION ----------------

    @Test
    @WithMockUser
    void getActiveVersion_shouldReturn200() throws Exception {

        UUID documentId = UUID.randomUUID();

        VersionResponse response = VersionResponse.builder().build();

        Mockito.when(versionService.getActiveVersion(documentId))
                .thenReturn(response);

        mockMvc.perform(get("/api/versions/{documentId}/active", documentId))
                .andExpect(status().isOk());
    }

    // ---------------- UPDATE FILE ----------------

    @Test
    @WithMockUser(roles = {"AUTHOR"})
    void updateDraftFile_shouldReturn200() throws Exception {

        UUID versionId = UUID.randomUUID();

        MockMultipartFile file =
                new MockMultipartFile("file", "test.txt", "text/plain", "updated".getBytes());

        VersionResponse response = VersionResponse.builder().build();

        Mockito.when(versionService.updateDraftFile(eq(versionId), any()))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/versions/{versionId}/file", versionId)
                        .file(file)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk());
    }

    // ---------------- EXPORT PDF ----------------

    @Test
    @WithMockUser
    void exportPdf_shouldReturnPdf() throws Exception {

        UUID versionId = UUID.randomUUID();

        byte[] pdf = "fake-pdf".getBytes();

        Mockito.when(versionService.exportToPdf(versionId))
                .thenReturn(pdf);

        mockMvc.perform(get("/api/versions/{versionId}/export/pdf", versionId))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}