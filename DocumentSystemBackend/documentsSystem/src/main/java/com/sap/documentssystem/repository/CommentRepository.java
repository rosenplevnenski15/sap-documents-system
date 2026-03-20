package com.sap.documentssystem.repository;

import com.sap.documentssystem.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByDocumentVersion_IdOrderByCreatedAtAsc(UUID versionId);
}