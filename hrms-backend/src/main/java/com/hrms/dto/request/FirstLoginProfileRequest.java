package com.hrms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FirstLoginProfileRequest {
    @NotBlank
    private String phoneNumber;

    private String bankName;
    private String accountHolderName;
    private String accountNumber;
    private String routingNumber;

    private String emergencyContactName;
    private String emergencyRelationship;
    private String emergencyPhoneNumber;
}
