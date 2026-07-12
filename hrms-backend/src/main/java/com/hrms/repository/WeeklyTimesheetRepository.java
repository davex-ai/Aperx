package com.hrms.repository;

import com.hrms.entity.WeeklyTimesheet;
import com.hrms.enums.TimesheetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyTimesheetRepository extends JpaRepository<WeeklyTimesheet, Long> {
    Optional<WeeklyTimesheet> findByEmployeeIdAndWeekStartDate(Long employeeId, LocalDate weekStartDate);
    List<WeeklyTimesheet> findByEmployeeIdOrderByWeekStartDateDesc(Long employeeId);
    List<WeeklyTimesheet> findByEmployeeCompanyIdAndStatusOrderByWeekStartDateAsc(Long companyId, TimesheetStatus status);
    List<WeeklyTimesheet> findByEmployeeManagerIdAndStatus(Long managerId, TimesheetStatus status);
    Optional<WeeklyTimesheet> findByEmployeeIdAndWeekStartDateAndStatus(Long employeeId, LocalDate weekStartDate, TimesheetStatus status);
    List<WeeklyTimesheet> findByEmployeeIdAndStatusAndWeekStartDateBetween(
            Long employeeId, TimesheetStatus status, LocalDate startInclusive, LocalDate endInclusive);
}
