package com.sap.documentssystem.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by",nullable = false)
    private User createdBy;

    @Setter(AccessLevel.NONE)
    @Column(name = "created_at",nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY)
    private List<DocumentVersion> versions;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}