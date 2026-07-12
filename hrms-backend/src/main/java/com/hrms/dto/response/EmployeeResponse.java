package com.hrms.dto.response;

import com.hrms.enums.PayType;
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
    private String personalEmail;
    private String companyEmail;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String department;
    private String jobTitle;
    private String salaryGrade;
    private LocalDate hireDate;
    private BigDecimal salaryAmount;
    private PayType payType;
    private BigDecimal hourlyRate;
    private String role;
    private Long managerId;
    private String managerName;
    private boolean active;
    private Boolean invitationSent;
    private String onboardingStage;
}
