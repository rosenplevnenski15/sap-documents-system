package com.sap.documentssystem.mapper;

import com.sap.documentssystem.dto.DocumentDto;
import com.sap.documentssystem.dto.VersionResponse;
import com.sap.documentssystem.model.DocumentVersion;

import javax.print.Doc;

public class VersionMapper {

    public static VersionResponse toResponse(DocumentVersion version) {

        return VersionResponse.builder()
                .id(version.getId())
                .versionNumber(version.getVersionNumber())
                .fileName(version.getFileName())
                .status(version.getStatus().name())
                .isActive(version.isActive())

                .createdAt(version.getCreatedAt())
                .approvedAt(version.getApprovedAt())

                .createdBy(MapUser.mapUser(version.getCreatedBy()))
                .approvedBy(version.getApprovedBy() != null
                        ? MapUser.mapUser(version.getApprovedBy())
                        : null)

                .document(DocumentDto.builder()
                        .id(version.getDocument().getId())
                        .title(version.getDocument().getTitle())
                        .build())
                .build();
    }
}