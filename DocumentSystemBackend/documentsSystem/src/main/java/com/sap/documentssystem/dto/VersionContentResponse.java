package com.sap.documentssystem.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class VersionContentResponse {

    private UUID id;
    private Integer versionNumber;
    private String fileName;
    private String status;
    private Boolean isActive;

    private String content;
}