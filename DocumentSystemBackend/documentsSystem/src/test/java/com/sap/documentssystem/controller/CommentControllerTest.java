package com.sap.documentssystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.documentssystem.dto.CommentResponse;
import com.sap.documentssystem.dto.CreateCommentRequest;
import com.sap.documentssystem.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommentControllerTest {

    private MockMvc mockMvc;

    private CommentService commentService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        commentService = Mockito.mock(CommentService.class);

        CommentController controller = new CommentController(commentService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
    }

    // ================= ADD COMMENT =================

    @Test
    void testAddComment_valid() throws Exception {
        UUID versionId = UUID.randomUUID();

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("This is a valid comment");

        CommentResponse response = CommentResponse.builder()
                .id(UUID.randomUUID())
                .content("This is a valid comment")
                .build();

        when(commentService.addComment(eq(versionId), any())).thenReturn(response);

        mockMvc.perform(post("/api/versions/" + versionId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("This is a valid comment"));

        verify(commentService, times(1)).addComment(eq(versionId), any());
    }

    @Test
    void testAddComment_invalid_emptyContent() throws Exception {
        UUID versionId = UUID.randomUUID();

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("");

        mockMvc.perform(post("/api/versions/" + versionId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(commentService, never()).addComment(any(), any());
    }

    // ================= GET COMMENTS =================

    @Test
    void testGetComments() throws Exception {
        UUID versionId = UUID.randomUUID();

        CommentResponse c1 = CommentResponse.builder()
                .id(UUID.randomUUID())
                .content("Comment 1")
                .build();

        CommentResponse c2 = CommentResponse.builder()
                .id(UUID.randomUUID())
                .content("Comment 2")
                .build();

        when(commentService.getComments(versionId)).thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/versions/" + versionId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("Comment 1"))
                .andExpect(jsonPath("$[1].content").value("Comment 2"));

        verify(commentService, times(1)).getComments(versionId);
    }
}