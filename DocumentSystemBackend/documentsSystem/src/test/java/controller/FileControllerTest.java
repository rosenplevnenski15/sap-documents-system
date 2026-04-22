package com.sap.documentssystem.controller;

import com.sap.documentssystem.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FileControllerTest {

    private MockMvc mockMvc;
    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = Mockito.mock(FileService.class);

        FileController controller = new FileController(fileService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    // ================= UPLOAD =================

    @Test
    void testUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "file content".getBytes()
        );

        String url = "http://localhost/files/test.txt";

        when(fileService.upload(any())).thenReturn(url);

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(content().string(url));

        verify(fileService, times(1)).upload(any());
    }
}