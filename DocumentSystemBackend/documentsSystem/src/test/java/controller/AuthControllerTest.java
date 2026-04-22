package controller;
import com.sap.documentssystem.controller.AuthController;
import com.sap.documentssystem.dto.LoginRequest;
import com.sap.documentssystem.dto.RegisterRequest;
import com.sap.documentssystem.service.AuthService;
import com.sap.documentssystem.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = com.sap.documentssystem.DocumentsSystemApplication.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    // ---------------- LOGIN ----------------
    @Test
    void shouldLoginSuccessfully() throws Exception {

        when(authService.login(any(LoginRequest.class)))
                .thenAnswer(invocation -> {
                    var res = mock(com.sap.documentssystem.dto.LoginResponse.class);
                    when(res.getAccessToken()).thenReturn("accessToken");
                    when(res.getRefreshToken()).thenReturn("refreshToken");
                    return res;
                });

        String json = """
        {
          "username": "user",
          "password": "Password1!"
        }
        """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.refreshToken").value("refreshToken"));

        verify(authService).login(any(LoginRequest.class));
    }

    // ---------------- REGISTER ----------------
    @Test
    void shouldRegisterSuccessfully() throws Exception {

        doNothing().when(userService).register(any(RegisterRequest.class));

        String json = """
        {
          "username": "user",
          "password": "Password1!",
          "email": "email@test.com"
        }
        """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered"));

        verify(userService).register(any(RegisterRequest.class));
    }

    // ---------------- REFRESH ----------------
    @Test
    void shouldRefreshTokenSuccessfully() throws Exception {

        when(authService.refreshToken(anyString()))
                .thenAnswer(invocation -> {
                    var res = mock(com.sap.documentssystem.dto.LoginResponse.class);
                    when(res.getAccessToken()).thenReturn("newAccess");
                    when(res.getRefreshToken()).thenReturn("newRefresh");
                    return res;
                });

        String json = """
        {
          "refreshToken": "someToken"
        }
        """;

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("newAccess"))
                .andExpect(jsonPath("$.refreshToken").value("newRefresh"));

        verify(authService).refreshToken(anyString());
    }

    // ---------------- LOGOUT ----------------
    @Test
    void shouldLogoutSuccessfully() throws Exception {

        doNothing().when(authService).logout();

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService).logout();
    }
}