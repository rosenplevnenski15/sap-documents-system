package com.sap.documentssystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class CommentResponse {

    private final UUID id;

    private final String content;

    private final LocalDateTime createdAt;

    private final UserDto user;
}