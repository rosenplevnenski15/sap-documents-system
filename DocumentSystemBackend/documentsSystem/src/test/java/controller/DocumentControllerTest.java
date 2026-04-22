package com.sap.documentssystem.controller;

import com.sap.documentssystem.dto.CompareResponse;
import com.sap.documentssystem.dto.DocumentResponse;
import com.sap.documentssystem.service.DocumentService;
import com.sap.documentssystem.service.DocumentVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DocumentControllerTest {

    private MockMvc mockMvc;

    private DocumentService documentService;
    private DocumentVersionService versionService;

    @BeforeEach
    void setUp() {
        documentService = Mockito.mock(DocumentService.class);
        versionService = Mockito.mock(DocumentVersionService.class);

        DocumentController controller = new DocumentController(documentService, versionService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    // ================= CREATE DOCUMENT =================

    @Test
    void testCreateDocument_valid() throws Exception {
        String title = "My Document";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello world".getBytes()
        );

        DocumentResponse response = DocumentResponse.builder()
                .id(UUID.randomUUID())
                .title(title)
                .build();

        when(documentService.createDocument(eq(title), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("title", title))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title));

        verify(documentService, times(1)).createDocument(eq(title), any());
    }

    @Test
    void testCreateDocument_invalid_emptyTitle() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello world".getBytes()
        );

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("title", ""))
                .andExpect(status().isBadRequest());

        verify(documentService, never()).createDocument(any(), any());
    }

    // ================= COMPARE =================

    @Test
    void testCompare() throws Exception {
        UUID documentId = UUID.randomUUID();

        CompareResponse response = CompareResponse.builder()
                .fileName1("v1.txt")
                .fileName2("v2.txt")
                .version1Content("old")
                .version2Content("new")
                .version1Number(1)
                .version2Number(2)
                .build();

        when(versionService.compareLatest(documentId)).thenReturn(response);

        mockMvc.perform(get("/api/documents/" + documentId + "/compare"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName1").value("v1.txt"))
                .andExpect(jsonPath("$.fileName2").value("v2.txt"));

        verify(versionService, times(1)).compareLatest(documentId);
    }
}