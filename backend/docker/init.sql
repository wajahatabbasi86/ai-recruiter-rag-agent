-- ─────────────────────────────────────────────────────────────────
-- AI Recruiter RAG Agent — Database Schema
-- ─────────────────────────────────────────────────────────────────

-- Tenants (multi-tenancy from day 1)
CREATE TABLE tenants (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- Candidate profiles
CREATE TABLE candidates (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL REFERENCES tenants(id),
    full_name        VARCHAR(255) NOT NULL,
    email            VARCHAR(255),
    phone            VARCHAR(50),
    source           VARCHAR(50) DEFAULT 'RESUME',  -- RESUME | LINKEDIN | REFERRAL | MANUAL
    raw_text         TEXT,
    -- Comma-separated Qdrant point IDs (one per resume chunk)
    -- Required for vector cleanup when candidate is deleted
    qdrant_point_ids TEXT,
    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW()
);

-- Job descriptions
CREATE TABLE jobs (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL REFERENCES tenants(id),
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    raw_text         TEXT,
    -- Comma-separated Qdrant point IDs (one per JD chunk)
    qdrant_point_ids TEXT,
    created_at       TIMESTAMP DEFAULT NOW()
);

-- Audit log — every AI query logged (required for EU AI Act compliance)
CREATE TABLE audit_logs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID,
    query_text     TEXT NOT NULL,
    matched_ids    TEXT,         -- comma-separated candidate IDs
    bias_detected  BOOLEAN DEFAULT FALSE,
    model_used     VARCHAR(100),
    created_at     TIMESTAMP DEFAULT NOW()
);

-- Recruiter feedback on AI matches (Phase 3)
CREATE TABLE match_feedback (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID,
    candidate_id            UUID REFERENCES candidates(id),
    job_id                  UUID REFERENCES jobs(id),
    score                   FLOAT,
    recruiter_rating        INTEGER CHECK (recruiter_rating BETWEEN 1 AND 5),
    human_review_requested  BOOLEAN DEFAULT FALSE,
    created_at              TIMESTAMP DEFAULT NOW()
);

-- Default dev tenant
INSERT INTO tenants (id, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'Dev Tenant');
