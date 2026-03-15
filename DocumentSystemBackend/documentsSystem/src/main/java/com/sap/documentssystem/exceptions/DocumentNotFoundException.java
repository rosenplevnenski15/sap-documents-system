package com.sap.documentssystem.exceptions;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException() {
        super("Document not found");
    }
}