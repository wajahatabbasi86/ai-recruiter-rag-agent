package com.airecruiter.repository;

import com.airecruiter.model.DuplicateCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for DuplicateCandidate entities.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Repository
public interface DuplicateCandidateRepository extends JpaRepository<DuplicateCandidate, UUID> {

    List<DuplicateCandidate> findByCandidateIdAndTenantId(UUID candidateId, UUID tenantId);

    List<DuplicateCandidate> findByTenantIdAndStatus(UUID tenantId, String status);
}