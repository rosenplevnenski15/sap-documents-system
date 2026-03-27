package com.sap.documentssystem.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class DocumentDto {

    private UUID id;

    private String title;
}