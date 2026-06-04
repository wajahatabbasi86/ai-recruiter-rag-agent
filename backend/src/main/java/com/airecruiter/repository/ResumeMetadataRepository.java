package com.airecruiter.repository;

import com.airecruiter.model.ResumeMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ResumeMetadata entities.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Repository
public interface ResumeMetadataRepository extends JpaRepository<ResumeMetadata, UUID> {

    List<ResumeMetadata> findByCandidateId(UUID candidateId);

    Optional<ResumeMetadata> findFirstByCandidateId(UUID candidateId);
}