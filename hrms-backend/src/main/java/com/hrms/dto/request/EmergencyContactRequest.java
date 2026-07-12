package com.hrms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmergencyContactRequest {
    @NotBlank
    private String contactName;
    private String relationship;
    @NotBlank
    private String phoneNumber;
    private String email;
}
