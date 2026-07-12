package com.hrms.repository;

import com.hrms.entity.Application;
import com.hrms.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByJobId(Long jobId);
    List<Application> findByStatus(ApplicationStatus status);
    long countByJobId(Long jobId);
}
