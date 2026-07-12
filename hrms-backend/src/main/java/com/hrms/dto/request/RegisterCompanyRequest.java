package com.hrms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterCompanyRequest {
    @NotBlank
    private String companyName;

    @NotBlank
    private String adminFirstName;

    @NotBlank
    private String adminLastName;

    @NotBlank @Email
    private String adminEmail;

    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    private String adminPassword;
}
