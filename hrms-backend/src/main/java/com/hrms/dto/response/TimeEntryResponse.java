package com.hrms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryResponse {
    private Long id;
    private LocalDateTime clockInAt;
    private Double clockInLat;
    private Double clockInLng;
    private LocalDateTime clockOutAt;
    private Double clockOutLat;
    private Double clockOutLng;
    private Double durationHours;
    private String notes;
    private boolean isActive;
}
