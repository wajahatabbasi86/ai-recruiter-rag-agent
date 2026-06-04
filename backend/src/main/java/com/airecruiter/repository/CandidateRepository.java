package com.airecruiter.repository;

import com.airecruiter.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Candidate entities.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Repository
public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    List<Candidate> findByTenantId(UUID tenantId);

    Optional<Candidate> findByEmailAddressAndTenantId(String emailAddress, UUID tenantId);

    Optional<Candidate> findByPhoneNumberAndTenantId(String phoneNumber, UUID tenantId);

    List<Candidate> findByTenantIdAndFullNameContainingIgnoreCase(UUID tenantId, String name);
}