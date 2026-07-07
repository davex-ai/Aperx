package com.hrms.repository;

import com.hrms.entity.LeaveRequest;
import com.hrms.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(LeaveStatus status);
    List<LeaveRequest> findByEmployeeManagerIdAndStatus(Long managerId, LeaveStatus status);
    long countByStatus(LeaveStatus status);
}
