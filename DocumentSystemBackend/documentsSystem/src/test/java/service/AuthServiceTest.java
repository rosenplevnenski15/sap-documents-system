package service;

import com.sap.documentssystem.dto.LoginRequest;
import com.sap.documentssystem.dto.LoginResponse;
import com.sap.documentssystem.entity.AuditAction;
import com.sap.documentssystem.entity.User;
import com.sap.documentssystem.repository.UserRepository;
import com.sap.documentssystem.security.JwtService;
import com.sap.documentssystem.service.AuditLogService;
import com.sap.documentssystem.service.AuthService;
import com.sap.documentssystem.service.CurrentUserService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private JwtService jwtService;
    @Mock private UserRepository userRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AuditLogService auditLogService;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks
    private AuthService authService;


    // --------------- TEST login() ---------------

    @Test
    void shouldLoginSuccessfully() {

        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("1234");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("john");

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(jwtService.generateToken(user))
                .thenReturn("access-token");

        when(jwtService.generateRefreshToken(user))
                .thenReturn("refresh-token");

        when(jwtService.getExpiration())
                .thenReturn(3600000L);

        // WHEN
        LoginResponse response = authService.login(request);

        // THEN
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        verify(authenticationManager)
                .authenticate(any());

        verify(auditLogService)
                .log(
                        eq(user),
                        eq(AuditAction.LOGIN),
                        eq("USER"),
                        eq(user.getId()),
                        anyMap()
                );
    }

    // --------------- TEST: refreshToken() ---------------

    @Test
    void shouldRefreshTokenSuccessfully() {

        String refreshToken = "refresh-token";

        User user = new User();
        user.setUsername("john");

        when(jwtService.extractUsername(refreshToken))
                .thenReturn("john");

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(jwtService.generateToken(user))
                .thenReturn("new-access-token");

        when(jwtService.getExpiration())
                .thenReturn(3600000L);

        LoginResponse response = authService.refreshToken(refreshToken);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
    }

    // --------------- TEST: logout() ----------

    @Test
    void shouldLogoutSuccessfully() {

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("john");

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        authService.logout();

        verify(auditLogService).log(
                eq(user),
                eq(AuditAction.LOGOUT),
                eq("USER"),
                eq(user.getId()),
                anyMap()
        );
    }
}
