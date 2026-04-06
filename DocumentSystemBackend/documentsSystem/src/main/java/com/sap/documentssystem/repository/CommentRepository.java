package com.sap.documentssystem.repository;

import com.sap.documentssystem.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query("""
    SELECT c FROM Comment c
    JOIN FETCH c.user
    WHERE c.id = :id
""")
    Optional<Comment> findByIdWithUser(UUID id);
    @Query("""
    SELECT c FROM Comment c
    JOIN FETCH c.user
    WHERE c.documentVersion.id = :versionId
    ORDER BY c.createdAt ASC
""")
    List<Comment> findAllWithUser(UUID versionId);
}