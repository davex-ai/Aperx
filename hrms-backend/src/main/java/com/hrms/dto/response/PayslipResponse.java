package com.hrms.dto.response;

import com.hrms.enums.PayType;
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
    private Long employeeId;
    private String employeeName;
    private Integer periodMonth;
    private Integer periodYear;
    private PayType payType;
    private Double hoursWorked;
    private BigDecimal grossSalary;
    private BigDecimal taxDeduction;
    private BigDecimal socialSecurityDeduction;
    private BigDecimal bonusAmount;
    private BigDecimal otherDeductions;
    private BigDecimal netSalary;
    private String downloadUrl;
}
