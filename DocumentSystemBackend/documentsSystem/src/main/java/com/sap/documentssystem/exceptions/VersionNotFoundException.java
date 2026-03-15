package com.sap.documentssystem.exceptions;

public class VersionNotFoundException extends RuntimeException {
    public VersionNotFoundException() {
        super("Version not found");
    }
}