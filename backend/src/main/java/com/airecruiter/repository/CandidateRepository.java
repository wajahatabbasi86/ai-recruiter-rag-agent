package com.airecruiter.repository;

import com.airecruiter.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    List<Candidate> findByTenantId(UUID tenantId);

    List<Candidate> findByQdrantPointIdsIn(List<String> pointIds);
}
