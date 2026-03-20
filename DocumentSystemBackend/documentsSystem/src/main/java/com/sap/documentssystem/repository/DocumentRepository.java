package com.sap.documentssystem.repository;

import com.sap.documentssystem.model.Document;
import com.sap.documentssystem.model.DocumentVersion;
import com.sap.documentssystem.model.VersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByCreatedBy_Id(UUID userId);
}
