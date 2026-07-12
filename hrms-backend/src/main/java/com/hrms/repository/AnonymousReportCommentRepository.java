package com.hrms.repository;

import com.hrms.entity.AnonymousReportComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnonymousReportCommentRepository extends JpaRepository<AnonymousReportComment, Long> {
    List<AnonymousReportComment> findByReportIdOrderByCreatedAtAsc(Long reportId);
    long countByReportId(Long reportId);
}
