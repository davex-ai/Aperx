package com.hrms.dto.request;

import lombok.Data;

@Data
public class ClockOutRequest {
    private Double latitude;
    private Double longitude;
    private String notes;
}
