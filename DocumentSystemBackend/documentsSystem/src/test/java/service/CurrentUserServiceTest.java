package service;

import com.sap.documentssystem.entity.User;
import com.sap.documentssystem.repository.UserRepository;
import com.sap.documentssystem.service.CurrentUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserService currentUserService;

    // ------------- helper за Authentication --------------
    private Authentication authentication(String username, boolean isAuthenticated) {
        Authentication auth = mock(Authentication.class);

        when(auth.isAuthenticated()).thenReturn(isAuthenticated);

        if (username != null) {
            when(auth.getName()).thenReturn(username);
        }

        return auth;
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---------------- getCurrentUser() ----------------

    @Test
    void shouldReturnCurrentUser() {

        String username = "john";
        User user = new User();
        user.setUsername(username);

        Authentication auth = authentication(username, true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        User result = currentUserService.getCurrentUser();

        assertThat(result).isEqualTo(user);
    }

    // ---------------- USER NOT FOUND ---------------

    @Test
    void shouldThrowWhenUserNotFound() {

        String username = "john";

        Authentication auth = authentication(username, true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                currentUserService.getCurrentUser()
        ).isInstanceOf(UsernameNotFoundException.class);
    }

    // -------------- NO AUTHENTICATION ---------------

    @Test
    void shouldThrowWhenNoAuthentication() {

        SecurityContextHolder.clearContext();

        assertThatThrownBy(() ->
                currentUserService.getCurrentUser()
        ).isInstanceOf(UsernameNotFoundException.class);
    }

    // -------------- NOT AUTHENTICATED ---------------

    @Test
    void shouldThrowWhenNotAuthenticated() {

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false); // само това ти трябва

        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() ->
                currentUserService.getCurrentUser()
        ).isInstanceOf(UsernameNotFoundException.class);
    }

    // --------------- NULL USERNAME ---------------

    @Test
    void shouldThrowWhenUsernameIsNull() {

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(null);
        when(auth.isAuthenticated()).thenReturn(true);

        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() ->
                currentUserService.getCurrentUser()
        ).isInstanceOf(UsernameNotFoundException.class);
    }

    // ---------------- getCurrentUsername() ----------------

    @Test
    void shouldReturnCurrentUsername() {

        Authentication auth = authentication("john", true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        String username = currentUserService.getCurrentUsername();

        assertThat(username).isEqualTo("john");
    }

    // ---------------- getCurrentUserId() ----------------

    @Test
    void shouldReturnCurrentUserId() {

        String username = "john";

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);

        Authentication auth = authentication(username, true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        UUID result = currentUserService.getCurrentUserId();

        assertThat(result).isEqualTo(user.getId());
    }
}
