package com.hrms.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayrollAdjustmentRequest {
    private Long employeeId;
    private BigDecimal bonusAmount;
    private BigDecimal deductionAmount;
    private String note;
}
