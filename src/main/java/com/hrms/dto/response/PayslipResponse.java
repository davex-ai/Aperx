package com.hrms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayslipResponse {
    private Long id;
    private Integer periodMonth;
    private Integer periodYear;
    private BigDecimal grossSalary;
    private BigDecimal taxDeduction;
    private BigDecimal otherDeductions;
    private BigDecimal netSalary;
    private String downloadUrl;
}
