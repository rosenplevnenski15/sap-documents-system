package com.sap.documentssystem.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DocumentResponse {

    private UUID id;

    private String title;

    private String createdBy;

    private LocalDateTime createdAt;

}