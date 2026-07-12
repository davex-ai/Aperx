package com.hrms.repository;

import com.hrms.entity.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {
    Optional<TimeEntry> findFirstByEmployeeIdAndClockOutAtIsNullOrderByClockInAtDesc(Long employeeId);
    List<TimeEntry> findByEmployeeIdAndClockInAtBetweenOrderByClockInAtAsc(Long employeeId, LocalDateTime start, LocalDateTime end);
    List<TimeEntry> findByEmployeeIdOrderByClockInAtDesc(Long employeeId);
}
