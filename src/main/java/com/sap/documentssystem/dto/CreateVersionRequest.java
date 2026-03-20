package com.sap.documentssystem.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class CreateVersionRequest {

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "S3 URL is required")
    private String s3Url;


}