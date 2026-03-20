package com.sap.documentssystem.exceptions;

public class InvalidVersionStateException extends RuntimeException {
    public InvalidVersionStateException(String message) {
        super(message);
    }
}