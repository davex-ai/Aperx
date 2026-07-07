package com.hrms.service;

import com.hrms.dto.request.PayrollRunRequest;
import com.hrms.dto.response.PayslipResponse;
import com.hrms.entity.Employee;
import com.hrms.entity.PayrollRun;
import com.hrms.entity.Payslip;
import com.hrms.enums.PayrollStatus;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.PayrollRunRepository;
import com.hrms.repository.PayslipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollService {

    private static final BigDecimal FLAT_TAX_RATE = new BigDecimal("0.15");

    private final PayrollRunRepository payrollRunRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;
    private final PdfService pdfService;
    @Transactional
    public List<PayslipResponse> runPayroll(Long processedByUserId, PayrollRunRequest request) {
        if (payrollRunRepository.findByPeriodMonthAndPeriodYear(request.getPeriodMonth(), request.getPeriodYear()).isPresent()) {
            throw new BadRequestException("Payroll for this period has already been processed");
        }

        Employee processedBy = employeeRepository.findByUserId(processedByUserId).orElse(null);

        PayrollRun run = PayrollRun.builder()
                .periodMonth(request.getPeriodMonth())
                .periodYear(request.getPeriodYear())
                .status(PayrollStatus.PROCESSED)
                .processedBy(processedBy)
                .processedAt(LocalDateTime.now())
                .build();
        run = payrollRunRepository.save(run);

        List<Employee> activeEmployees = employeeRepository.findAll().stream()
                .filter(e -> e.getUser() != null && Boolean.TRUE.equals(e.getUser().getIsActive()))
                .collect(Collectors.toList());

        final PayrollRun savedRun = run;
        return activeEmployees.stream()
                .map(employee -> generatePayslip(savedRun, employee, request.getPeriodMonth(), request.getPeriodYear()))
                .collect(Collectors.toList());
    }

    private PayslipResponse generatePayslip(PayrollRun run, Employee employee, int month, int year) {
        BigDecimal gross = employee.getSalaryAmount();
        BigDecimal tax = gross.multiply(FLAT_TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal otherDeductions = BigDecimal.ZERO;
        BigDecimal net = gross.subtract(tax).subtract(otherDeductions);

        Payslip payslip = Payslip.builder()
                .payrollRun(run)
                .employee(employee)
                .grossSalary(gross)
                .taxDeduction(tax)
                .otherDeductions(otherDeductions)
                .netSalary(net)
                .build();
        payslip = payslipRepository.save(payslip);

        try {
            String pdfPath = pdfService.generatePayslipPdf(payslip, month, year);
            payslip.setPdfPath(pdfPath);
            payslip = payslipRepository.save(payslip);
        } catch (Exception ex) {
            log.error("Failed to generate payslip PDF for employee {}: {}", employee.getId(), ex.getMessage());
        }

        return toResponse(payslip, month, year);
    }

    public List<PayslipResponse> getMyPayslips(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        return payslipRepository.findByEmployeeIdOrderByIdDesc(employee.getId()).stream()
                .map(p -> toResponse(p, p.getPayrollRun().getPeriodMonth(), p.getPayrollRun().getPeriodYear()))
                .collect(Collectors.toList());
    }

    public Payslip getPayslipForDownload(Long payslipId, Long requestingUserId, boolean isPrivileged) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found"));

        if (!isPrivileged) {
            Employee employee = employeeRepository.findByUserId(requestingUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
            if (!payslip.getEmployee().getId().equals(employee.getId())) {
                throw new BadRequestException("You do not have access to this payslip");
            }
        }
        return payslip;
    }

    private PayslipResponse toResponse(Payslip p, int month, int year) {
        return PayslipResponse.builder()
                .id(p.getId())
                .periodMonth(month)
                .periodYear(year)
                .grossSalary(p.getGrossSalary())
                .taxDeduction(p.getTaxDeduction())
                .otherDeductions(p.getOtherDeductions())
                .netSalary(p.getNetSalary())
                .downloadUrl("/api/payroll/payslips/" + p.getId() + "/download")
                .build();
    }
}
