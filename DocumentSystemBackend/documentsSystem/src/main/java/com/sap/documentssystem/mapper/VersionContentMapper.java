package com.sap.documentssystem.mapper;

import com.sap.documentssystem.dto.VersionContentResponse;
import com.sap.documentssystem.entity.DocumentVersion;

public class VersionContentMapper {

    public static VersionContentResponse toResponse(DocumentVersion version, String content) {
        return VersionContentResponse.builder()
                .id(version.getId())
                .versionNumber(version.getVersionNumber())
                .fileName(version.getFileName())
                .status(version.getStatus().name())
                .isActive(version.isActive())
                .content(content)
                .build();
    }
}