package com.hrms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String companySlug;
    private String token;
    private String email;
    private String role;
    private boolean mustCompleteOnboarding;
    private Long employeeId;
    private String fullName;
    private Long companyId;
    private String companyName;
}
