package com.hrms.dto.request;

import lombok.Data;

@Data
public class ClockInRequest {
    private Double latitude;
    private Double longitude;
    private String notes;
}
