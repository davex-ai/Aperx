package com.hrms.dto.response;

import com.hrms.enums.LeaveStatus;
import com.hrms.enums.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private LeaveType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String certificateUrl;
    private LeaveStatus status;
    private String reviewComment;
    private String approvedByName;
    private LocalDateTime createdAt;
}
