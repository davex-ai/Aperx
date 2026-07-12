package com.hrms.repository;

import com.hrms.entity.JobQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobQuestionRepository extends JpaRepository<JobQuestion, Long> {
    List<JobQuestion> findByJobPostingIdOrderByDisplayOrderAsc(Long jobPostingId);
    void deleteByJobPostingId(Long jobPostingId);
}
