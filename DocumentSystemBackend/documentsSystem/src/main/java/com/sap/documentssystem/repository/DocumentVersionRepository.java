package com.sap.documentssystem.repository;

import com.sap.documentssystem.model.DocumentVersion;
import com.sap.documentssystem.model.VersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {

    Optional<DocumentVersion> findTopByDocument_IdOrderByVersionNumberDesc(UUID documentId);

    List<DocumentVersion> findByDocument_IdOrderByVersionNumberDesc(UUID documentId);

    Optional<DocumentVersion> findTopByDocument_IdAndStatusOrderByVersionNumberDesc(
            UUID documentId,
            VersionStatus status
    );
}
