package com.sap.documentssystem.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HomeResponse {
    private String application;
    private String status;
}