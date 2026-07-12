package com.hrms.repository;

import com.hrms.entity.ApplicationAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationAnswerRepository extends JpaRepository<ApplicationAnswer, Long> {
    List<ApplicationAnswer> findByApplicationIdOrderByJobQuestionDisplayOrderAsc(Long applicationId);
}
