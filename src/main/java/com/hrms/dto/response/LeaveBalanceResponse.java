package com.hrms.dto.response;

import com.hrms.enums.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceResponse {
    private LeaveType type;
    private Double totalDays;
    private Double usedDays;
    private Double remainingDays;
}
