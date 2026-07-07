package com.hrms.dto.request;

import com.hrms.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApplicationStatusUpdateRequest {
    @NotNull
    private ApplicationStatus status;
}
