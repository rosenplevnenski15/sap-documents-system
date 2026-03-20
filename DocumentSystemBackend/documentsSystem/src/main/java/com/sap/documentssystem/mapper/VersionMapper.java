package com.sap.documentssystem.mapper;

import com.sap.documentssystem.dto.VersionResponse;
import com.sap.documentssystem.model.DocumentVersion;

import javax.print.Doc;

public class VersionMapper {

    public static VersionResponse toResponse(DocumentVersion version) {

        return VersionResponse.builder()
                .id(version.getId())
                .documentId(version.getDocument().getId())
                .versionNumber(version.getVersionNumber())
                .fileName(version.getFileName())
                .status(version.getStatus().name())
                .isActive(version.isActive())
                .createdBy(version.getCreatedBy().getUsername())
                .createdAt(version.getCreatedAt())
                .approvedBy(version.getApprovedBy()!=null
                        ? version.getApprovedBy().getUsername()
                        : null)
                .approvedAt(version.getApprovedAt())
                .build();
    }
}