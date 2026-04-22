package com.sap.documentssystem.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TestControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TestController controller = new TestController();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    // ================= ADMIN =================

    @Test
    void testAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/test/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("ADMIN ACCESS GRANTED"));
    }

    // ================= AUTHOR =================

    @Test
    void testAuthorEndpoint() throws Exception {
        mockMvc.perform(get("/api/test/author"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("AUTHOR ACCESS GRANTED"));
    }

    // ================= REVIEWER =================

    @Test
    void testReviewerEndpoint() throws Exception {
        mockMvc.perform(get("/api/test/reviewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("REVIEWER ACCESS GRANTED"));
    }

    // ================= READER =================

    @Test
    void testReaderEndpoint() throws Exception {
        mockMvc.perform(get("/api/test/reader"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("READER ACCESS GRANTED"));
    }
}