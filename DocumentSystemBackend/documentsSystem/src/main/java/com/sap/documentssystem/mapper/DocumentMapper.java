package com.sap.documentssystem.mapper;

import com.sap.documentssystem.dto.DocumentResponse;
import com.sap.documentssystem.model.Document;

public class DocumentMapper {

    public static DocumentResponse toResponse(Document document) {

        return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .createdBy(document.getCreatedBy().getUsername())
                .createdAt(document.getCreatedAt())
                .build();
    }

}