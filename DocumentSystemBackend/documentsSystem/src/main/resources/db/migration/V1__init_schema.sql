-- =========================
-- EXTENSIONS
-- =========================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =========================
-- ENUMS
-- =========================

CREATE TYPE user_role AS ENUM (
    'READER',
    'AUTHOR',
    'REVIEWER',
    'ADMIN'
    );

CREATE TYPE version_status AS ENUM (
    'DRAFT',
    'IN_REVIEW',
    'APPROVED',
    'REJECTED'
    );

CREATE TYPE audit_action AS ENUM (
    'USER_REGISTERED',
    'USER_ROLE_CHANGED',
    'USER_DEACTIVATED',

    'CREATE_DOCUMENT',
    'UPDATE_DOCUMENT',
    'DELETE_DOCUMENT',

    'CREATE_VERSION',
    'EDIT_DRAFT',
    'SUBMIT_FOR_REVIEW',

    'APPROVE_VERSION',
    'REJECT_VERSION',

    'ADD_COMMENT',

    'DOWNLOAD_DOCUMENT',
    'EXPORT_DOCUMENT',

    'LOGIN',
    'LOGOUT'
    );

-- =========================
-- USERS
-- =========================

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

                       username VARCHAR(50) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,

                       role user_role NOT NULL DEFAULT 'READER',

                       is_active BOOLEAN DEFAULT TRUE,

                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP
);

-- =========================
-- DOCUMENTS
-- =========================

CREATE TABLE documents (
                           id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

                           title VARCHAR(255) NOT NULL,

                           created_by UUID NOT NULL REFERENCES users(id),
                           created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =========================
-- DOCUMENT VERSIONS
-- =========================

CREATE TABLE document_versions (
                                   id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

                                   document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,

                                   version_number INT NOT NULL,

                                   file_name VARCHAR(255) NOT NULL,
                                   s3_url TEXT NOT NULL,

                                   status version_status NOT NULL DEFAULT 'DRAFT',

                                   is_active BOOLEAN NOT NULL DEFAULT FALSE,

                                   created_by UUID NOT NULL REFERENCES users(id),
                                   created_at TIMESTAMP NOT NULL DEFAULT NOW(),

                                   approved_by UUID REFERENCES users(id),
                                   approved_at TIMESTAMP,

                                   CONSTRAINT unique_version_per_document
                                       UNIQUE (document_id, version_number)
);

-- =========================
-- COMMENTS (REVIEWER FEATURE)
-- =========================

CREATE TABLE comments (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

                          document_version_id UUID NOT NULL
                              REFERENCES document_versions(id) ON DELETE CASCADE,

                          user_id UUID NOT NULL REFERENCES users(id),

                          content TEXT NOT NULL,

                          created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =========================
-- AUDIT LOG (ENTERPRISE LEVEL)
-- =========================

CREATE TABLE audit_log (
                           id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

                           user_id UUID REFERENCES users(id),

                           action audit_action NOT NULL,

                           entity_type VARCHAR(50), -- DOCUMENT / VERSION / USER / COMMENT
                           entity_id UUID,

                           details JSONB, -- допълнителна информация (пример: { "oldRole": "READER", "newRole": "AUTHOR" })

                           created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =========================
-- INDEXES (PERFORMANCE)
-- =========================

CREATE INDEX idx_documents_created_by
    ON documents(created_by);

CREATE INDEX idx_document_versions_document_id
    ON document_versions(document_id);

CREATE INDEX idx_document_versions_status
    ON document_versions(status);

CREATE INDEX idx_document_versions_active
    ON document_versions(document_id)
    WHERE is_active = TRUE;

CREATE INDEX idx_comments_version
    ON comments(document_version_id);

CREATE INDEX idx_audit_entity
    ON audit_log(entity_type, entity_id);

CREATE INDEX idx_audit_user
    ON audit_log(user_id);

-- =========================
-- CONSTRAINT: ONLY ONE ACTIVE VERSION
-- =========================

CREATE UNIQUE INDEX uq_one_active_version_per_document
    ON document_versions(document_id)
    WHERE is_active = TRUE;