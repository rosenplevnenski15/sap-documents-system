package com.sap.documentssystem.mapper;

import com.sap.documentssystem.dto.CommentResponse;
import com.sap.documentssystem.entity.Comment;

public class CommentMapper {

    public static CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .user(MapUser.mapUser(comment.getUser()))
                .build();
    }

}