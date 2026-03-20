package com.sap.documentssystem.service;

import com.sap.documentssystem.model.Role;
import com.sap.documentssystem.model.User;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    public void canCreateDocument(User user) {
        if (user.getRole() != Role.AUTHOR && user.getRole() != Role.ADMIN) {
            throw forbidden("Only AUTHOR or ADMIN can create documents");
        }
    }

    public void canCreateVersion(User user) {
        if (user.getRole() != Role.AUTHOR && user.getRole() != Role.ADMIN) {
            throw forbidden("Only AUTHOR or ADMIN can create versions");
        }
    }

    public void canEditDraft(User user) {
        if (user.getRole() != Role.AUTHOR && user.getRole() != Role.ADMIN) {
            throw forbidden("Only AUTHOR or ADMIN can edit drafts");
        }
    }

    public void canSubmitForReview(User user) {
        if (user.getRole() != Role.AUTHOR && user.getRole() != Role.ADMIN) {
            throw forbidden("Only AUTHOR or ADMIN can submit for review");
        }
    }

    public void canApprove(User user) {
        if (user.getRole() != Role.REVIEWER && user.getRole() != Role.ADMIN) {
            throw forbidden("Only REVIEWER or ADMIN can approve versions");
        }
    }

    public void canReject(User user) {
        if (user.getRole() != Role.REVIEWER && user.getRole() != Role.ADMIN) {
            throw forbidden("Only REVIEWER or ADMIN can reject versions");
        }
    }

    public void canComment(User user) {
        if (user.getRole() != Role.REVIEWER && user.getRole() != Role.ADMIN) {
            throw forbidden("Only REVIEWER or ADMIN can add comments");
        }
    }

    public void canRead(User user) {
        if (user.getRole() == null) {
            throw forbidden("Invalid user role");
        }
    }

    private RuntimeException forbidden(String message) {
        return new RuntimeException(message);
    }
}