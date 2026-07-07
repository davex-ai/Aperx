package com.hrms.repository;

import com.hrms.entity.JobPosting;
import com.hrms.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    List<JobPosting> findByStatus(JobStatus status);
    List<JobPosting> findByDepartment(String department);
}
