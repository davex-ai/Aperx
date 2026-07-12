package com.hrms.repository;

import com.hrms.entity.AnnouncementComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementCommentRepository extends JpaRepository<AnnouncementComment, Long> {
    List<AnnouncementComment> findByAnnouncementIdOrderByCreatedAtAsc(Long announcementId);
    long countByAnnouncementId(Long announcementId);
}
