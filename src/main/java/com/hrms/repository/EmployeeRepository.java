package com.hrms.repository;

import com.hrms.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUserId(Long userId);
    Optional<Employee> findByUserEmail(String email);
    List<Employee> findByManagerId(Long managerId);
    List<Employee> findByDepartment(String department);
}
