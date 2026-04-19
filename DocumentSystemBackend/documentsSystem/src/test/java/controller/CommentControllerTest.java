package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.documentssystem.controller.CommentController;
import com.sap.documentssystem.dto.CommentResponse;
import com.sap.documentssystem.dto.CreateCommentRequest;
import com.sap.documentssystem.dto.UserDto;
import com.sap.documentssystem.service.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    // ---------------- ADD COMMENT ----------------

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void addComment_shouldReturnCreated() throws Exception {

        UUID versionId = UUID.randomUUID();

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("Nice document");

        CommentResponse response = CommentResponse.builder()
                .id(UUID.randomUUID())
                .content("Nice document")
                .createdAt(LocalDateTime.now())
                .user(UserDto.builder()
                        .id(UUID.randomUUID())
                        .username("testuser")
                        .build())
                .build();

        when(commentService.addComment(eq(versionId), any(CreateCommentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/versions/{versionId}/comments", versionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Nice document"));

        verify(commentService, times(1))
                .addComment(eq(versionId), any(CreateCommentRequest.class));
    }

    // ---------------- GET COMMENTS ----------------

    @Test
    @WithMockUser
    void getComments_shouldReturnList() throws Exception {

        UUID versionId = UUID.randomUUID();

        CommentResponse c1 = CommentResponse.builder()
                .id(UUID.randomUUID())
                .content("First")
                .createdAt(LocalDateTime.now())
                .user(UserDto.builder()
                        .id(UUID.randomUUID())
                        .username("u1")
                        .build())
                .build();

        CommentResponse c2 = CommentResponse.builder()
                .id(UUID.randomUUID())
                .content("Second")
                .createdAt(LocalDateTime.now())
                .user(UserDto.builder()
                        .id(UUID.randomUUID())
                        .username("u2")
                        .build())
                .build();

        when(commentService.getComments(versionId))
                .thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/versions/{versionId}/comments", versionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("First"))
                .andExpect(jsonPath("$[1].content").value("Second"));

        verify(commentService, times(1)).getComments(versionId);
    }
}