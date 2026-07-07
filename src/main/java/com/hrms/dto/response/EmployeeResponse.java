package com.hrms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String department;
    private String jobTitle;
    private String salaryGrade;
    private LocalDate hireDate;
    private BigDecimal salaryAmount;
    private String role;
    private Long managerId;
    private String managerName;
    private boolean active;
    private Boolean invitationSent;
}
