package com.sap.documentssystem.controller;

import com.sap.documentssystem.dto.VersionContentResponse;
import com.sap.documentssystem.dto.VersionResponse;
import com.sap.documentssystem.service.DocumentVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DocumentVersionControllerTest {

    private MockMvc mockMvc;
    private DocumentVersionService versionService;

    @BeforeEach
    void setUp() {
        versionService = Mockito.mock(DocumentVersionService.class);

        DocumentVersionController controller = new DocumentVersionController(versionService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    // ================= CREATE VERSION =================

    @Test
    void testCreateVersion() throws Exception {
        UUID documentId = UUID.randomUUID();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "doc.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "content".getBytes()
        );

        VersionResponse response = VersionResponse.builder()
                .id(UUID.randomUUID())
                .versionNumber(1)
                .status("DRAFT")
                .fileName("doc.txt")
                .isActive(true)
                .build();

        when(versionService.createVersion(eq(documentId), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/versions/documents/" + documentId + "/versions")
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(versionService).createVersion(eq(documentId), any());
    }

    // ================= WORKFLOW =================

    @Test
    void testSubmitForReview() throws Exception {
        UUID versionId = UUID.randomUUID();

        VersionResponse response = VersionResponse.builder()
                .id(versionId)
                .status("IN_REVIEW")
                .build();

        when(versionService.submitForReview(versionId)).thenReturn(response);

        mockMvc.perform(post("/api/versions/" + versionId + "/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));

        verify(versionService).submitForReview(versionId);
    }

    @Test
    void testApproveVersion() throws Exception {
        UUID versionId = UUID.randomUUID();

        VersionResponse response = VersionResponse.builder()
                .id(versionId)
                .status("APPROVED")
                .build();

        when(versionService.approveVersion(versionId)).thenReturn(response);

        mockMvc.perform(post("/api/versions/" + versionId + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(versionService).approveVersion(versionId);
    }

    @Test
    void testRejectVersion() throws Exception {
        UUID versionId = UUID.randomUUID();

        VersionResponse response = VersionResponse.builder()
                .id(versionId)
                .status("REJECTED")
                .build();

        when(versionService.rejectVersion(versionId)).thenReturn(response);

        mockMvc.perform(post("/api/versions/" + versionId + "/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        verify(versionService).rejectVersion(versionId);
    }

    // ================= GET =================

    @Test
    void testGetVersions() throws Exception {
        UUID documentId = UUID.randomUUID();

        VersionResponse v1 = VersionResponse.builder().id(UUID.randomUUID()).build();
        VersionResponse v2 = VersionResponse.builder().id(UUID.randomUUID()).build();

        when(versionService.getVersions(documentId)).thenReturn(List.of(v1, v2));

        mockMvc.perform(get("/api/versions/" + documentId + "/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(versionService).getVersions(documentId);
    }

    @Test
    void testGetActiveVersion() throws Exception {
        UUID documentId = UUID.randomUUID();

        VersionResponse response = VersionResponse.builder()
                .id(UUID.randomUUID())
                .isActive(true)
                .build();

        when(versionService.getActiveVersion(documentId)).thenReturn(response);

        mockMvc.perform(get("/api/versions/" + documentId + "/active"))
                .andExpect(status().isOk());

        verify(versionService).getActiveVersion(documentId);
    }

    // ================= UPDATE FILE =================

    @Test
    void testUpdateDraftFile() throws Exception {
        UUID versionId = UUID.randomUUID();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "updated.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "updated content".getBytes()
        );

        VersionResponse response = VersionResponse.builder()
                .id(versionId)
                .build();

        when(versionService.updateDraftFile(eq(versionId), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/versions/" + versionId + "/file")
                        .file(file)
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        }))
                .andExpect(status().isOk());

        verify(versionService).updateDraftFile(eq(versionId), any());
    }

    // ================= PDF =================

    @Test
    void testExportPdf() throws Exception {
        UUID versionId = UUID.randomUUID();

        byte[] pdf = "pdf".getBytes();

        when(versionService.exportToPdf(versionId)).thenReturn(pdf);

        mockMvc.perform(get("/api/versions/" + versionId + "/export/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        verify(versionService).exportToPdf(versionId);
    }

    // ================= CONTENT =================

    @Test
    void testGetVersionContent() throws Exception {
        UUID versionId = UUID.randomUUID();

        VersionContentResponse response = VersionContentResponse.builder()
                .id(versionId)
                .content("file content")
                .build();

        when(versionService.getVersionContent(versionId)).thenReturn(response);

        mockMvc.perform(get("/api/versions/" + versionId + "/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("file content"));

        verify(versionService).getVersionContent(versionId);
    }
}