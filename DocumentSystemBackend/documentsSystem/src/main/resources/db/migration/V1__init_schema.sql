CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE version_status AS ENUM (
    'DRAFT',
    'IN_REVIEW',
    'APPROVED',
    'REJECTED'
);

CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL,
                       is_active BOOLEAN DEFAULT TRUE,
                       created_at TIMESTAMP,
                       updated_at TIMESTAMP
);

CREATE TABLE documents (
                           id UUID PRIMARY KEY,
                           title VARCHAR(255) NOT NULL,
                           created_by UUID NOT NULL REFERENCES users(id),
                           created_at TIMESTAMP NOT NULL
);

CREATE TABLE document_versions (
                                   id UUID PRIMARY KEY,
                                   document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                                   version_number INT NOT NULL,
                                   file_name VARCHAR(255) NOT NULL,
                                   s3_url TEXT NOT NULL,
                                   status version_status NOT NULL,
                                   is_active BOOLEAN NOT NULL DEFAULT FALSE,
                                   created_by UUID NOT NULL REFERENCES users(id),
                                   created_at TIMESTAMP NOT NULL,
                                   approved_by UUID REFERENCES users(id),
                                   approved_at TIMESTAMP,

                                   CONSTRAINT unique_version_per_document
                                       UNIQUE (document_id, version_number)
);

CREATE TABLE audit_log (
                           id UUID PRIMARY KEY,
                           user_id UUID REFERENCES users(id),
                           action VARCHAR(50),
                           entity_type VARCHAR(50),
                           entity_id UUID,
                           created_at TIMESTAMP
);

CREATE INDEX idx_document_versions_document_id
    ON document_versions(document_id);

CREATE INDEX idx_document_versions_status
    ON document_versions(status);

CREATE INDEX idx_documents_created_by
    ON documents(created_by);

CREATE INDEX idx_audit_log_entity
    ON audit_log(entity_id);

CREATE UNIQUE INDEX uq_one_active_version_per_document
    ON document_versions(document_id)
    WHERE is_active = TRUE;