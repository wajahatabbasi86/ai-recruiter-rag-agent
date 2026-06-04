package com.airecruiter.repository;

import com.airecruiter.model.MatchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for MatchResult entities.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, UUID> {

    List<MatchResult> findByJobIdOrderByCandidateRank(UUID jobId);

    Page<MatchResult> findByJobId(UUID jobId, Pageable pageable);

    List<MatchResult> findByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    @Query("SELECT m FROM MatchResult m WHERE m.jobId = :jobId AND m.finalMatchScore >= :minScore ORDER BY m.finalMatchScore DESC")
    List<MatchResult> findHighScoreMatchesForJob(@Param("jobId") UUID jobId,
                                                 @Param("minScore") Float minScore);

    void deleteByJobId(UUID jobId);
}