package com.airecruiter.repository;

import com.airecruiter.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Job entities.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByTenantIdAndJobStatus(UUID tenantId, String jobStatus);

    List<Job> findByTenantId(UUID tenantId);
}