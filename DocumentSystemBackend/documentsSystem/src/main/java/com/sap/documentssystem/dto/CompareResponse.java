package com.sap.documentssystem.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompareResponse {

    private String fileName1;
    private String fileName2;

    private String version1Content;
    private String version2Content;

    private Integer version1Number;
    private Integer version2Number;
}