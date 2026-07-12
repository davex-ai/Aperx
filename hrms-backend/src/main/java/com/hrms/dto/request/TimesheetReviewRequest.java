package com.hrms.dto.request;

import com.hrms.enums.TimesheetStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TimesheetReviewRequest {
    @NotNull
    private TimesheetStatus status;
    private String reviewComment;
}
