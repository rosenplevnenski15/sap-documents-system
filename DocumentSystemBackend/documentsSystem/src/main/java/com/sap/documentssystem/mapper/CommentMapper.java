package com.sap.documentssystem.mapper;

import com.sap.documentssystem.dto.CommentResponse;
import com.sap.documentssystem.model.Comment;

public class CommentMapper {
    public static CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .userId(comment.getUser().getId())
                .build();
    }
}