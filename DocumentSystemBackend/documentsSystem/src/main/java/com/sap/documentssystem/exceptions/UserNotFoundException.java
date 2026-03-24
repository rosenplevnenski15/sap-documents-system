package com.sap.documentssystem.exceptions;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("User Not Found");
    }
    public UserNotFoundException(UUID userId) {
        super("User not found with id: " + userId);
    }
}