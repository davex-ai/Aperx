package com.hrms.repository;

import com.hrms.entity.AnonymousReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnonymousReportRepository extends JpaRepository<AnonymousReport, Long> {
    List<AnonymousReport> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    Optional<AnonymousReport> findByIdAndCompanyId(Long id, Long companyId);
}
