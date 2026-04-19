package service;

import com.sap.documentssystem.entity.Role;
import com.sap.documentssystem.entity.User;
import com.sap.documentssystem.exceptions.AccessDeniedException;
import com.sap.documentssystem.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.function.Consumer;

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

    private void assertAllowed(Consumer<User> action, Role... roles) {
        for (Role role : roles) {
            action.accept(user(role));
        }
    }

    private void assertDenied(Consumer<User> action, Role role, String message) {
        assertThatThrownBy(() -> action.accept(user(role)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(message);
    }

    // ---------------- CREATE DOCUMENT ----------------

    @Test
    void shouldAllowCreateDocument() {
        assertAllowed(authorizationService::canCreateDocument, Role.AUTHOR, Role.ADMIN);
    }

    @Test
    void shouldDenyCreateDocument() {
        assertDenied(
                authorizationService::canCreateDocument,
                Role.REVIEWER,
                "Only AUTHOR or ADMIN can create documents"
        );
    }

    // ---------------- APPROVE ----------------

    @Test
    void shouldAllowApprove() {
        assertAllowed(authorizationService::canApprove, Role.REVIEWER, Role.ADMIN);
    }

    @Test
    void shouldDenyApprove() {
        assertDenied(
                authorizationService::canApprove,
                Role.AUTHOR,
                "Only REVIEWER or ADMIN can approve versions"
        );
    }

    // ---------------- COMMENT ----------------

    @Test
    void shouldAllowComment() {
        assertAllowed(authorizationService::canComment, Role.REVIEWER, Role.ADMIN);
    }

    @Test
    void shouldDenyComment() {
        assertDenied(
                authorizationService::canComment,
                Role.AUTHOR,
                "Only REVIEWER or ADMIN can add comments"
        );
    }

    // ---------------- COMPARE ----------------

    @Test
    void shouldAllowCompare() {
        assertAllowed(authorizationService::canCompare,
                Role.ADMIN, Role.AUTHOR, Role.REVIEWER);
    }

    @Test
    void shouldDenyCompareWhenRoleIsNull() {
        assertThatThrownBy(() ->
                authorizationService.canCompare(user(null))
        ).isInstanceOf(AccessDeniedException.class);
    }

    // ---------------- READ ----------------

    @Test
    void shouldAllowRead() {
        assertAllowed(authorizationService::canRead, Role.ADMIN);
    }

    @Test
    void shouldDenyReadWhenRoleIsNull() {
        assertThatThrownBy(() ->
                authorizationService.canRead(user(null))
        )
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Invalid user role");
    }

    // --------------- CREATE VERSION ----------------

    @Test
    void shouldAllowCreateVersion() {
        assertAllowed(authorizationService::canCreateVersion, Role.AUTHOR, Role.ADMIN);
    }

    @Test
    void shouldDenyCreateVersion() {
        assertDenied(authorizationService::canCreateVersion, Role.REVIEWER,
                "Only AUTHOR or ADMIN can create versions");
    }

    // --------------- EDIT DRAFT ---------------

    @Test
    void shouldAllowEditDraft() {
        assertAllowed(authorizationService::canEditDraft, Role.ADMIN, Role.AUTHOR);
    }

    @Test
    void shouldDenyEditDraft() {
        assertDenied(authorizationService::canEditDraft, Role.REVIEWER,
                "Only AUTHOR or ADMIN can edit drafts");
    }


    // --------------- SUBMIT FOR REVIEW ---------------

    @Test
    void shouldAllowSubmit() {
        assertAllowed(authorizationService::canSubmitForReview, Role.AUTHOR, Role.ADMIN);
    }

    @Test
    void shouldDenySubmit() {
        assertDenied(authorizationService::canSubmitForReview, Role.REVIEWER,
                "Only AUTHOR or ADMIN can submit for review");
    }

    // --------------- REJECT ---------------

    @Test
    void shouldAllowReject() {
        assertAllowed(authorizationService::canReject, Role.REVIEWER, Role.ADMIN);
    }

    @Test
    void shouldDenyReject() {
        assertDenied(authorizationService::canReject, Role.AUTHOR,
                "Only REVIEWER or ADMIN can reject versions");
    }
}