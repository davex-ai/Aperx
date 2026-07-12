package com.hrms.repository;

import com.hrms.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByCompanyIdOrderByIsPinnedDescCreatedAtDesc(Long companyId);
    Optional<Announcement> findByIdAndCompanyId(Long id, Long companyId);
}
