package com.airecruiter.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * All request/response DTOs for the AI Recruiter API.
 */
public class RecruiterDtos {

    // ── Resume Upload ─────────────────────────────────────────────

    @Data
    @Builder
    public static class ResumeUploadResponse {
        private UUID candidateId;
        private String name;
        private String message;
        /** EU AI Act: label all AI-processed outputs */
        private final String processedBy = "AI-assisted (nomic-embed-text via Ollama)";
    }

    // ── Candidate Update ──────────────────────────────────────────

    @Data
    public static class CandidateUpdateRequest {
        private String fullName;
        private String email;
        private String phone;
        private String source; // RESUME | LINKEDIN | REFERRAL | MANUAL
    }

    // ── RAG Query ─────────────────────────────────────────────────

    @Data
    @Builder
    public static class RagQueryRequest {
        private String query;   // e.g. "Find Java developers with 5 years experience"
        private int topK;       // how many candidates to retrieve (default 5)
    }

    @Data
    @Builder
    public static class RagQueryResponse {
        private String query;
        private String aiAnswer;
        private List<CandidateMatch> matches;
        private boolean biasDetected;
        private String auditLogId;
        /** EU AI Act: clearly label AI-generated output */
        private final String generatedBy    = "AI-generated (llama3.1:8b via Ollama)";
        private final String humanReviewUrl = "/api/rag/request-human-review";
    }

    @Data
    @Builder
    public static class CandidateMatch {
        private UUID candidateId;
        private String name;
        private float similarityScore;      // 0.0 – 1.0
        private String relevantExcerpt;     // Relevant chunk from their resume
    }

    // ── Candidate Profile ─────────────────────────────────────────

    @Data
    @Builder
    public static class CandidateResponse {
        private UUID id;
        private String fullName;
        private String email;
        private String source;
        private String createdAt;
    }
}
