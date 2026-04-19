package controller;

import com.sap.documentssystem.controller.UserController;
import com.sap.documentssystem.dto.ApiResponse;
import com.sap.documentssystem.dto.UserDto;
import com.sap.documentssystem.entity.Role;
import com.sap.documentssystem.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    // ---------------- CHANGE ROLE ----------------

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void changeRole_shouldReturn200() throws Exception {

        UUID userId = UUID.randomUUID();

        mockMvc.perform(put("/api/users/{userId}/role", userId)
                        .param("role", "AUTHOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role updated"));

        Mockito.verify(userService).changeRole(eq(userId), eq(Role.AUTHOR));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void changeRole_shouldReturn403() throws Exception {

        UUID userId = UUID.randomUUID();

        mockMvc.perform(put("/api/users/{userId}/role", userId)
                        .param("role", "AUTHOR"))
                .andExpect(status().isForbidden());
    }

    // ---------------- DEACTIVATE USER ----------------

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void deactivateUser_shouldReturn200() throws Exception {

        UUID userId = UUID.randomUUID();

        mockMvc.perform(put("/api/users/{userId}/deactivate", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deactivated"));

        Mockito.verify(userService).deactivateUser(eq(userId));
    }

    @Test
    @WithMockUser(roles = {"AUTHOR"})
    void deactivateUser_shouldReturn403() throws Exception {

        UUID userId = UUID.randomUUID();

        mockMvc.perform(put("/api/users/{userId}/deactivate", userId))
                .andExpect(status().isForbidden());
    }

    // ---------------- GET ALL USERS ----------------

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getAllUsers_shouldReturn200_andList() throws Exception {

        UserDto user = UserDto.builder()
                .id(UUID.randomUUID())
                .username("testUser")
                .build();

        Mockito.when(userService.getAllUsers())
                .thenReturn(List.of(user));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("testUser"));
    }

    @Test
    @WithMockUser(roles = {"READER"})
    void getAllUsers_shouldReturn403() throws Exception {

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }
}