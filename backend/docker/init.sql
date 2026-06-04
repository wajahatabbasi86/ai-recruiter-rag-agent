-- ============================================================
-- AI Recruiter RAG Agent — Phase 1 Database Schema
-- ============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ────────────────────────────────────────────────────────────
-- candidates
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS candidates (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id         UUID NOT NULL,
    full_name         VARCHAR(255) NOT NULL,
    email_address     VARCHAR(255),
    phone_number      VARCHAR(50),
    source            VARCHAR(50),
    raw_text          TEXT,
    qdrant_point_ids  TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_candidates_tenant_id   ON candidates(tenant_id);
CREATE INDEX IF NOT EXISTS idx_candidates_email       ON candidates(email_address);

-- ────────────────────────────────────────────────────────────
-- jobs
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS jobs (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id                   UUID NOT NULL,
    job_title                   VARCHAR(255) NOT NULL,
    job_description             TEXT,
    required_skills             TEXT,
    minimum_experience_years    INTEGER,
    preferred_experience_years  INTEGER,
    required_education_level    VARCHAR(100),
    required_location           VARCHAR(255),
    required_tools              TEXT,
    required_frameworks         TEXT,
    required_databases          TEXT,
    original_file_path          VARCHAR(500),
    raw_extracted_text          TEXT,
    job_status                  VARCHAR(50) DEFAULT 'active',
    qdrant_point_ids            TEXT,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_jobs_tenant_id     ON jobs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_jobs_status        ON jobs(job_status);

-- ────────────────────────────────────────────────────────────
-- resume_metadata
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS resume_metadata (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    candidate_id                UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    technical_skills            TEXT,
    total_experience_years      INTEGER,
    previous_companies          TEXT,
    job_titles_held             TEXT,
    education_level             VARCHAR(100),
    university_name             VARCHAR(255),
    fields_of_study             TEXT,
    current_location            VARCHAR(255),
    country                     VARCHAR(100),
    willing_to_relocate         BOOLEAN,
    tools                       TEXT,
    frameworks                  TEXT,
    databases                   TEXT,
    latest_company_name         VARCHAR(255),
    latest_job_title            VARCHAR(255),
    latest_role_end_date        VARCHAR(50),
    current_employment_status   VARCHAR(50),
    extracted_email             VARCHAR(255),
    extracted_phone             VARCHAR(50),
    linked_in_profile_url       VARCHAR(500),
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_resume_metadata_candidate_id ON resume_metadata(candidate_id);

-- ────────────────────────────────────────────────────────────
-- match_results
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS match_results (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id                      UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    candidate_id                UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    vector_similarity_score     FLOAT,
    structured_matching_score   FLOAT,
    recency_bonus_points        FLOAT,
    location_bonus_points       FLOAT,
    final_match_score           FLOAT,
    candidate_rank              INTEGER,
    matched_skills              TEXT,
    matched_frameworks          TEXT,
    matched_tools               TEXT,
    matched_databases           TEXT,
    identified_gaps             TEXT,
    match_reasons               TEXT,
    match_status                VARCHAR(50) DEFAULT 'PENDING',
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_match_results_job_id       ON match_results(job_id);
CREATE INDEX IF NOT EXISTS idx_match_results_candidate_id ON match_results(candidate_id);
CREATE INDEX IF NOT EXISTS idx_match_results_status       ON match_results(match_status);
CREATE INDEX IF NOT EXISTS idx_match_results_score        ON match_results(final_match_score DESC);

-- ────────────────────────────────────────────────────────────
-- duplicate_candidates
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS duplicate_candidates (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    candidate_id            UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    duplicate_candidate_id  UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    tenant_id               UUID NOT NULL,
    similarity_score        DOUBLE PRECISION,
    detection_reason        VARCHAR(100),
    status                  VARCHAR(50) DEFAULT 'PENDING_REVIEW',
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_duplicate_candidates_tenant ON duplicate_candidates(tenant_id);
