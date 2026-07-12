package com.hrms.repository;

import com.hrms.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    List<Payslip> findByEmployeeIdOrderByIdDesc(Long employeeId);
    List<Payslip> findByPayrollRunId(Long payrollRunId);
}
