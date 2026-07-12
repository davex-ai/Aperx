package com.hrms.dto.response;

import com.hrms.enums.TimesheetStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyTimesheetResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private Double totalHours;
    private TimesheetStatus status;
    private LocalDateTime submittedAt;
    private String reviewedByName;
    private String reviewComment;
    private List<TimeEntryResponse> entries;
}
