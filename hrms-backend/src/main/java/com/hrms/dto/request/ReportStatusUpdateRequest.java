package com.hrms.dto.request;

import com.hrms.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportStatusUpdateRequest {
    @NotNull
    private ReportStatus status;
}
