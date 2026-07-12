package com.hrms.repository;

import com.hrms.entity.LeaveBalance;
import com.hrms.enums.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, Integer year);
    Optional<LeaveBalance> findByEmployeeIdAndTypeAndYear(Long employeeId, LeaveType type, Integer year);
}
