package service;

import com.sap.documentssystem.dto.LoginRequest;
import com.sap.documentssystem.dto.RegisterRequest;
import com.sap.documentssystem.dto.UserDto;
import com.sap.documentssystem.entity.Role;
import com.sap.documentssystem.entity.User;
import com.sap.documentssystem.exceptions.UserAlreadyExistsException;
import com.sap.documentssystem.exceptions.UserAlreadyInActiveException;
import com.sap.documentssystem.exceptions.UserNotFoundException;
import com.sap.documentssystem.repository.UserRepository;
import com.sap.documentssystem.service.AuditLogService;
import com.sap.documentssystem.service.CurrentUserService;
import com.sap.documentssystem.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private UserService userService;

    // ---------------- REGISTER ----------------

    @Test
    void shouldRegisterUserSuccessfully() {

        RegisterRequest request = RegisterRequest.builder()
                .username("reader_deni")
                .password("reasDeshjj@143")
                .build();

        when(userRepository.existsByUsername("test")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");

        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> {
                    User u = inv.getArgument(0);
                    u.setId(UUID.randomUUID());
                    return u;
                });

        userService.register(request);

        verify(userRepository).save(any(User.class));
        verify(auditLogService).log(any(), any(), anyString(), any(), anyMap());
    }

    @Test
    void shouldThrowWhenUserAlreadyExists() {

        RegisterRequest request = RegisterRequest.builder()
                .username("reader_deni")
                .build();

        when(userRepository.existsByUsername("reader_deni")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    // ---------------- CHANGE ROLE ----------------

    @Test
    void shouldChangeRoleSuccessfully() {

        UUID userId = UUID.randomUUID();

        User admin = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .build();

        User user = User.builder()
                .id(userId)
                .username("user")
                .role(Role.READER)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.changeRole(userId, Role.ADMIN);

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository).save(user);
        verify(auditLogService).log(any(), any(), anyString(), any(), anyMap());
    }

    @Test
    void shouldThrowWhenUserNotFoundOnChangeRole() {

        UUID userId = UUID.randomUUID();

        when(currentUserService.getCurrentUser()).thenReturn(new User());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeRole(userId, Role.ADMIN))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void shouldThrowWhenRoleIsSame() {

        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .role(Role.READER)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(new User());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.changeRole(userId, Role.READER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User already has this role");
    }

    // ---------------- DEACTIVATE USER ----------------

    @Test
    void shouldDeactivateUserSuccessfully() {

        UUID userId = UUID.randomUUID();

        User admin = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .build();

        User user = User.builder()
                .id(userId)
                .username("user")
                .isActive(true)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deactivateUser(userId);

        assertThat(user.isActive()).isFalse();
        verify(auditLogService).log(any(), any(), anyString(), any(), anyMap());
    }

    @Test
    void shouldThrowWhenUserNotFoundOnDeactivate() {

        UUID userId = UUID.randomUUID();

        when(currentUserService.getCurrentUser()).thenReturn(new User());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivateUser(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void shouldThrowWhenUserAlreadyInactive() {

        UUID userId = UUID.randomUUID();

        User admin = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .build();

        User user = User.builder()
                .id(userId)
                .isActive(false)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.deactivateUser(userId))
                .isInstanceOf(UserAlreadyInActiveException.class);
    }

    @Test
    void shouldThrowWhenAdminDeactivatesHimself() {

        UUID adminId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(admin);

        User sameUser = User.builder()
                .id(adminId)
                .username("admin")
                .isActive(true)
                .build();

        when(userRepository.findById(adminId)).thenReturn(Optional.of(sameUser));

        assertThatThrownBy(() -> userService.deactivateUser(adminId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Admin cannot deactivate himself");
    }

    // ---------------- GET ALL USERS ----------------

    @Test
    void shouldReturnAllUsers() {

        when(userRepository.findAll()).thenReturn(List.of(
                User.builder().id(UUID.randomUUID()).username("u1").build(),
                User.builder().id(UUID.randomUUID()).username("u2").build()
        ));

        List<UserDto> result = userService.getAllUsers();

        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(2);
    }
}