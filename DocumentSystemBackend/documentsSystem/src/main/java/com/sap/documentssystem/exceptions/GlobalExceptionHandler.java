package com.sap.documentssystem.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DocumentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleDocumentNotFound() {
        return "Document not found";
    }

    @ExceptionHandler(VersionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleVersionNotFound() {
        return "Version not found";
    }

    @ExceptionHandler(InvalidVersionStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalidState(InvalidVersionStateException ex) {
        return ex.getMessage();
    }
}