package com.hrms.repository;

import com.hrms.entity.LeaveRequest;
import com.hrms.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    List<LeaveRequest> findByEmployeeCompanyIdAndStatusOrderByCreatedAtDesc(Long companyId, LeaveStatus status);
    List<LeaveRequest> findByEmployeeManagerIdAndStatus(Long managerId, LeaveStatus status);
    long countByEmployeeCompanyIdAndStatus(Long companyId, LeaveStatus status);
}
