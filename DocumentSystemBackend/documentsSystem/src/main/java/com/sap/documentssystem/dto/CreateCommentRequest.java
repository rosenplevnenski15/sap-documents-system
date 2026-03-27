package com.sap.documentssystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentRequest {

    @NotBlank(message = "Comment content is required")
    @Size(min = 5, max = 1000, message = "Comment must be between 5 and 1000 characters")
    private String content;
}