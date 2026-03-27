package com.sap.documentssystem.repository;

import com.sap.documentssystem.model.DocumentVersion;
import com.sap.documentssystem.model.VersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {

    Optional<DocumentVersion> findTopByDocument_IdOrderByVersionNumberDesc(UUID documentId);

    List<DocumentVersion> findByDocument_IdOrderByVersionNumberDesc(UUID documentId);

    Optional<DocumentVersion> findTopByDocument_IdAndStatusOrderByVersionNumberDesc(
            UUID documentId,
            VersionStatus status
    );

    Optional<DocumentVersion> findByDocument_IdAndIsActiveTrue(UUID documentId);

    @Modifying
    @Query("""
        update DocumentVersion dv
        set dv.isActive = false
        where dv.document.id = :documentId
          and dv.isActive = true
          and dv.id <> :versionId
    """)
    int deactivateOtherActiveVersions(UUID documentId, UUID versionId);

    List<DocumentVersion> findByDocument_IdAndIsActiveTrueAndStatus(
            UUID documentId,
            VersionStatus status
    );
    List<DocumentVersion> findByDocument_IdAndStatus(
            UUID documentId,
            VersionStatus status
    );

    @Query("""
    SELECT v FROM DocumentVersion v
    JOIN FETCH v.createdBy
    JOIN FETCH v.document
    LEFT JOIN FETCH v.approvedBy
    WHERE v.id = :id
""")
    Optional<DocumentVersion> findFullById(UUID id);

    @Query("""
    SELECT v FROM DocumentVersion v
    JOIN FETCH v.createdBy
    JOIN FETCH v.document
    LEFT JOIN FETCH v.approvedBy
    WHERE v.document.id = :documentId
""")
    List<DocumentVersion> findAllFullByDocumentId(UUID documentId);

    @Query("""
    SELECT v FROM DocumentVersion v
    JOIN FETCH v.createdBy
    JOIN FETCH v.document
    LEFT JOIN FETCH v.approvedBy
    WHERE v.document.id = :documentId
    AND v.isActive = true
""")
    Optional<DocumentVersion> findActiveFullByDocumentId(UUID documentId);

    @Query("""
    SELECT v FROM DocumentVersion v
    JOIN FETCH v.createdBy
    JOIN FETCH v.document
    LEFT JOIN FETCH v.approvedBy
    WHERE v.document.id = :documentId
    AND v.status = :status
    ORDER BY v.versionNumber DESC
""")
    List<DocumentVersion> findInReviewFull(UUID documentId, VersionStatus status);
}