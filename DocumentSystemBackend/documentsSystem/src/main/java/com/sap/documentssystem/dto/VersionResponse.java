package com.sap.documentssystem.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VersionResponse {

    private UUID id;

    private UUID documentId;

    private Integer versionNumber;

    private String fileName;

    private String status;

    private Boolean isActive;

    private String createdBy;

    private LocalDateTime createdAt;

    private String approvedBy;

    private LocalDateTime approvedAt;

}