package controller;

import com.sap.documentssystem.controller.TestController;
import com.sap.documentssystem.service.FileService;
import com.sap.documentssystem.service.DocumentService;
import com.sap.documentssystem.service.DocumentVersionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TestController.class)
class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private DocumentVersionService documentVersionService;

    // ---------------- ADMIN ----------------

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void admin_shouldReturn200() throws Exception {

        mockMvc.perform(get("/api/test/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("ADMIN ACCESS GRANTED"));
    }

    @Test
    @WithMockUser(roles = {"AUTHOR"})
    void admin_shouldReturn403_forNonAdmin() throws Exception {

        mockMvc.perform(get("/api/test/admin"))
                .andExpect(status().isForbidden());
    }

    // ---------------- AUTHOR ----------------

    @Test
    @WithMockUser(roles = {"AUTHOR"})
    void author_shouldReturn200() throws Exception {

        mockMvc.perform(get("/api/test/author"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("AUTHOR ACCESS GRANTED"));
    }

    @Test
    @WithMockUser(roles = {"READER"})
    void author_shouldReturn403() throws Exception {

        mockMvc.perform(get("/api/test/author"))
                .andExpect(status().isForbidden());
    }

    // ---------------- REVIEWER ----------------

    @Test
    @WithMockUser(roles = {"REVIEWER"})
    void reviewer_shouldReturn200() throws Exception {

        mockMvc.perform(get("/api/test/reviewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("REVIEWER ACCESS GRANTED"));
    }

    @Test
    @WithMockUser(roles = {"AUTHOR"})
    void reviewer_shouldReturn403() throws Exception {

        mockMvc.perform(get("/api/test/reviewer"))
                .andExpect(status().isForbidden());
    }

    // ---------------- READER ----------------

    @Test
    @WithMockUser(roles = {"READER"})
    void reader_shouldReturn200() throws Exception {

        mockMvc.perform(get("/api/test/reader"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("READER ACCESS GRANTED"));
    }

    @Test
    @WithMockUser(roles = {"AUTHOR"})
    void reader_shouldReturn403() throws Exception {

        mockMvc.perform(get("/api/test/reader"))
                .andExpect(status().isForbidden());
    }

    // ---------------- NO AUTH ----------------

    @Test
    void shouldReturn401_whenNotAuthenticated() throws Exception {

        mockMvc.perform(get("/api/test/admin"))
                .andExpect(status().isUnauthorized());
    }
}