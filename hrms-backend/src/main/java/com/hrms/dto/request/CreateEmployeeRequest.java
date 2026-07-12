package com.hrms.dto.request;

import com.hrms.enums.PayType;
import com.hrms.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateEmployeeRequest {
    @NotBlank @Email
    private String personalEmail;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String phoneNumber;

    @NotBlank
    private String department;

    @NotBlank
    private String jobTitle;

    private String salaryGrade;

    @NotNull
    private LocalDate hireDate;

    @NotNull
    private BigDecimal salaryAmount;

    @NotNull
    private PayType payType;

    private BigDecimal hourlyRate;

    private Long managerId;

    @NotNull
    private UserRole role;
}
