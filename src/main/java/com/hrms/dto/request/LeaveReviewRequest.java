package com.hrms.dto.request;

import com.hrms.enums.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeaveReviewRequest {
    @NotNull
    private LeaveStatus status;
    private String reviewComment;
}
