package controller;

import com.sap.documentssystem.controller.DocumentController;
import com.sap.documentssystem.dto.DocumentResponse;
import com.sap.documentssystem.dto.CompareResponse;
import com.sap.documentssystem.service.DocumentService;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private DocumentVersionService versionService;

    // ---------------- CREATE DOCUMENT ----------------

    @Test
    @WithMockUser(roles = {"AUTHOR"})
    void createDocument_shouldReturn201_andValidJson() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile("file", "test.txt", "text/plain", "hello world".getBytes());

        MockMultipartFile title =
                new MockMultipartFile("title", "", "text/plain", "My Document".getBytes());

        DocumentResponse response = DocumentResponse.builder()
                .id(UUID.randomUUID())
                .title("My Document")
                .createdBy(null)
                .createdAt(LocalDateTime.now())
                .build();

        Mockito.when(documentService.createDocument(eq("My Document"), any()))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .file(title)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("My Document"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void createDocument_shouldReturn403_forbidden() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        MockMultipartFile title =
                new MockMultipartFile("title", "", "text/plain", "My Document".getBytes());

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .file(title))
                .andExpect(status().isForbidden());
    }

    // ---------------- COMPARE ----------------

    @Test
    @WithMockUser(roles = {"REVIEWER"})
    void compare_shouldReturn200_andJson() throws Exception {

        UUID documentId = UUID.randomUUID();

        CompareResponse response = CompareResponse.builder()
                .build();

        Mockito.when(versionService.compareLatest(documentId))
                .thenReturn(response);

        mockMvc.perform(get("/api/documents/{id}/compare", documentId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void compare_shouldReturn403_forbidden() throws Exception {

        UUID documentId = UUID.randomUUID();

        mockMvc.perform(get("/api/documents/{id}/compare", documentId))
                .andExpect(status().isForbidden());
    }
}