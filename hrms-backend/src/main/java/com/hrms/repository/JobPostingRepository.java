package com.hrms.repository;

import com.hrms.entity.JobPosting;
import com.hrms.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    List<JobPosting> findByCompanyIdAndStatus(Long companyId, JobStatus status);
    List<JobPosting> findByCompanyId(Long companyId);
    Optional<JobPosting> findByIdAndCompanyId(Long id, Long companyId);
}
