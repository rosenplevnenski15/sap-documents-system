package com.sap.documentssystem.repository;

import com.sap.documentssystem.model.Document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByCreatedBy_Id(UUID userId);
    Page<Document> findByCreatedBy_Id(UUID userId, Pageable pageable);
    Page<Document> findByTitleContainingIgnoreCase(String query, Pageable pageable);
    @Query("SELECT d FROM Document d WHERE d.createdBy.id = :userId AND LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Document> searchByCreatorAndQuery(@Param("userId") UUID userId, @Param("query") String query, Pageable pageable);

}
