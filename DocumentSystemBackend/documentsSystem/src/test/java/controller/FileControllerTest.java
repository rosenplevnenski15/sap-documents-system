package controller;

import com.sap.documentssystem.controller.FileController;
import com.sap.documentssystem.service.FileService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    // ---------------- UPLOAD SUCCESS ----------------

    @Test
    @WithMockUser(roles = {"AUTHOR"})
    void uploadFile_shouldReturn201_andUrl() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "test.txt",
                        "text/plain",
                        "hello world".getBytes()
                );

        Mockito.when(fileService.upload(any()))
                .thenReturn("http://fake-url.com/file/test.txt");

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(content().string("http://fake-url.com/file/test.txt"));
    }

    // ---------------- UPLOAD FORBIDDEN ----------------

    @Test
    @WithMockUser(roles = {"USER"})
    void uploadFile_shouldReturn403() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "test.txt",
                        "text/plain",
                        "hello world".getBytes()
                );

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file))
                .andExpect(status().isForbidden());
    }
}