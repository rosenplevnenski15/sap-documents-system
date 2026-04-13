package service;

import com.sap.documentssystem.entity.Role;
import com.sap.documentssystem.entity.User;
import com.sap.documentssystem.exceptions.AccessDeniedException;
import com.sap.documentssystem.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationServiceTest {

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
    }

    private User user(Role role) {
        User u = new User();
        u.setRole(role);
        return u;
    }

    // ---------------- CREATE DOCUMENT ----------------

    @Test
    void shouldAllowAuthorToCreateDocument() {
        authorizationService.canCreateDocument(user(Role.AUTHOR));
    }

    @Test
    void shouldAllowAdminToCreateDocument() {
        authorizationService.canCreateDocument(user(Role.ADMIN));
    }

    @Test
    void shouldDenyReviewerToCreateDocument() {
        assertThatThrownBy(() ->
                authorizationService.canCreateDocument(user(Role.REVIEWER))
        )
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only AUTHOR or ADMIN can create documents");
    }

    // ---------------- APPROVE ----------------

    @Test
    void shouldAllowReviewerToApprove() {
        authorizationService.canApprove(user(Role.REVIEWER));
    }

    @Test
    void shouldAllowAdminToApprove() {
        authorizationService.canApprove(user(Role.ADMIN));
    }

    @Test
    void shouldDenyAuthorToApprove() {
        assertThatThrownBy(() ->
                authorizationService.canApprove(user(Role.AUTHOR))
        )
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only REVIEWER or ADMIN can approve versions");
    }

    // ---------------- COMMENT ----------------

    @Test
    void shouldAllowReviewerToComment() {
        authorizationService.canComment(user(Role.REVIEWER));
    }

    @Test
    void shouldDenyAuthorToComment() {
        assertThatThrownBy(() ->
                authorizationService.canComment(user(Role.AUTHOR))
        )
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only REVIEWER or ADMIN can add comments");
    }

    // ---------------- COMPARE ----------------

    @Test
    void shouldAllowAllRolesToCompare() {
        authorizationService.canCompare(user(Role.ADMIN));
        authorizationService.canCompare(user(Role.AUTHOR));
        authorizationService.canCompare(user(Role.REVIEWER));
    }

    // ---------------- READ ----------------

    @Test
    void shouldAllowReadWhenRoleExists() {
        authorizationService.canRead(user(Role.ADMIN));
    }

    @Test
    void shouldDenyReadWhenRoleIsNull() {
        assertThatThrownBy(() ->
                authorizationService.canRead(user(null))
        )
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Invalid user role");
    }
}