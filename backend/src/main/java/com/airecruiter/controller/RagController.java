package com.airecruiter.controller;

import com.airecruiter.dto.RecruiterDtos.*;
import com.airecruiter.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the RAG query pipeline.
 *
 * POST /api/rag/query  — recruiter asks a natural language question
 *
 * Example request body:
 * {
 *   "query": "Find Java developers with Spring Boot experience and 3+ years",
 *   "topK": 5
 * }
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    /**
     * Main RAG query endpoint.
     *
     * curl -X POST http://localhost:8080/api/rag/query \
     *   -H "Content-Type: application/json" \
     *   -d '{"query": "Java developers with microservices experience", "topK": 5}'
     */
    @PostMapping("/query")
    public ResponseEntity<RagQueryResponse> query(@RequestBody RagQueryRequest request) {
        log.info("RAG query received: {}", request.getQuery());

        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        RagQueryResponse response = ragService.query(request);
        return ResponseEntity.ok(response);
    }

    /**
     * EU AI Act compliance endpoint.
     * Candidates and recruiters can request a human review of any AI decision.
     */
    @PostMapping("/request-human-review")
    public ResponseEntity<String> requestHumanReview(@RequestParam String auditLogId) {
        log.info("Human review requested for audit log: {}", auditLogId);
        // TODO: Notify a human recruiter (email/Slack in Phase 3)
        return ResponseEntity.ok(
            "Human review request logged for audit ID: " + auditLogId +
            ". A recruiter will review within 24 hours."
        );
    }
}
