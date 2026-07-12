package com.hrms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPreviewResponse {
    private Integer periodMonth;
    private Integer periodYear;
    private boolean alreadyProcessed;
    private List<PayslipResponse> lines;
    private BigDecimal totalGross;
    private BigDecimal totalNet;
    private int employeeCount;
}
