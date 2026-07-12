package com.hrms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompleteSignupRequest {
    @NotBlank
    private String token;

    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;

    private String securityQuestion;
    private String securityAnswer;
}
