package com.hrms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignCompanyEmailRequest {
    @NotBlank @Email
    private String companyEmail;
}
