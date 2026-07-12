package com.hrms.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String phoneNumber;
    private String bankName;
    private String accountHolderName;
    private String accountNumber;
    private String routingNumber;
}
