package com.airecruiter.controller;

import com.airecruiter.dto.MatchRequestDto;
import com.airecruiter.dto.MatchResultDto;
import com.airecruiter.service.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Resume-to-Job Matching.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    /**
     * Trigger matching for a job.
     * POST /api/matches/job/{jobId}/match-skills
     *
     * @param jobId the job ID
     * @param matchRequestDto the match request DTO
     * @return response entity
     */
    @PostMapping("/job/{jobId}/match-skills")
    public ResponseEntity<String> triggerMatching(
            @PathVariable UUID jobId,
            @RequestBody MatchRequestDto matchRequestDto) {
        
        log.info("Match trigger request received for job: {}", jobId);
        
        try {
            matchingService.initiateMatching(jobId, matchRequestDto);
            return ResponseEntity.accepted()
                    .body("Matching process initiated for job: " + jobId);
        } catch (RuntimeException runtimeException) {
            log.error("Job not found: {}", jobId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception exception) {
            log.error("Error during matching: {}", exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get shortlist for a job.
     * GET /api/matches/job/{jobId}/shortlist
     *
     * @param jobId the job ID
     * @param minimumScore optional minimum score filter
     * @return response entity with list of match results
     */
    @GetMapping("/job/{jobId}/shortlist")
    public ResponseEntity<List<MatchResultDto>> getShortlist(
            @PathVariable UUID jobId,
            @RequestParam(value = "minimumScore", required = false) Float minimumScore) {
        
        log.info("Shortlist request received for job: {}", jobId);
        
        try {
            MatchRequestDto matchRequestDto = MatchRequestDto.builder()
                    .minimumMatchScore(minimumScore)
                    .build();
            
            List<MatchResultDto> shortlist = matchingService.getShortlist(jobId, matchRequestDto);
            return ResponseEntity.ok(shortlist);
        } catch (Exception exception) {
            log.error("Error fetching shortlist: {}", exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get paginated shortlist for a job.
     * GET /api/matches/job/{jobId}/shortlist-paginated
     *
     * @param jobId the job ID
     * @param pageNumber the page number (0-indexed)
     * @param pageSize the page size
     * @return response entity with paginated match results
     */
    @GetMapping("/job/{jobId}/shortlist-paginated")
    public ResponseEntity<Page<MatchResultDto>> getShortlistPaginated(
            @PathVariable UUID jobId,
            @RequestParam(value = "page", defaultValue = "0") Integer pageNumber,
            @RequestParam(value = "size", defaultValue = "10") Integer pageSize) {
        
        log.info("Paginated shortlist request received for job: {}, page: {}, size: {}",
                jobId, pageNumber, pageSize);
        
        try {
            Page<MatchResultDto> shortlist = matchingService.getShortlistPaginated(jobId, pageNumber, pageSize);
            return ResponseEntity.ok(shortlist);
        } catch (Exception exception) {
            log.error("Error fetching paginated shortlist: {}", exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get match details for a specific candidate and job.
     * GET /api/matches/job/{jobId}/candidate/{candidateId}
     *
     * @param jobId the job ID
     * @param candidateId the candidate ID
     * @return response entity with match result DTO
     */
    @GetMapping("/job/{jobId}/candidate/{candidateId}")
    public ResponseEntity<MatchResultDto> getMatchDetails(
            @PathVariable UUID jobId,
            @PathVariable UUID candidateId) {
        
        log.info("Match details request received for job: {}, candidate: {}", jobId, candidateId);
        
        try {
            MatchResultDto matchResult = matchingService.getMatchDetails(jobId, candidateId);
            return ResponseEntity.ok(matchResult);
        } catch (RuntimeException runtimeException) {
            log.error("Match not found for job: {}, candidate: {}", jobId, candidateId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception exception) {
            log.error("Error fetching match details: {}", exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update match status.
     * PUT /api/matches/{matchId}/status
     *
     * @param matchId the match ID
     * @param newStatus the new status
     * @return response entity
     */
    @PutMapping("/{matchId}/status")
    public ResponseEntity<String> updateMatchStatus(
            @PathVariable UUID matchId,
            @RequestParam String newStatus) {
        
        log.info("Match status update request received for match: {}, new status: {}", matchId, newStatus);
        
        try {
            matchingService.updateMatchStatus(matchId, newStatus);
            return ResponseEntity.ok("Match status updated successfully to: " + newStatus);
        } catch (RuntimeException runtimeException) {
            log.error("Match not found: {}", matchId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception exception) {
            log.error("Error updating match status: {}", exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
