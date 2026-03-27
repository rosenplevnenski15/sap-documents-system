package com.sap.documentssystem.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VersionResponse {

    private UUID id;

    private Integer versionNumber;

    private String fileName;

    private String status;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime approvedAt;

    private UserDto createdBy;
    private UserDto approvedBy;

    private DocumentDto document;
}