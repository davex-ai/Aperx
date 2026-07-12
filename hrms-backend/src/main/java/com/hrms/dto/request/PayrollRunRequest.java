package com.hrms.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PayrollRunRequest {
    @NotNull @Min(1) @Max(12)
    private Integer periodMonth;

    @NotNull
    private Integer periodYear;

    private List<PayrollAdjustmentRequest> adjustments;
}
