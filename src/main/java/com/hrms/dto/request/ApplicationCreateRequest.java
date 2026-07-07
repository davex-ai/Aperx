package com.hrms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApplicationCreateRequest {
    @NotBlank
    private String candidateName;
    @NotBlank @Email
    private String candidateEmail;
    @NotBlank
    private String resumeUrl;
}
