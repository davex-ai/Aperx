package com.hrms.service;

import com.hrms.dto.request.PayrollAdjustmentRequest;
import com.hrms.dto.request.PayrollRunRequest;
import com.hrms.dto.response.PayrollPreviewResponse;
import com.hrms.dto.response.PayslipResponse;
import com.hrms.entity.Company;
import com.hrms.entity.Employee;
import com.hrms.entity.PayrollRun;
import com.hrms.entity.Payslip;
import com.hrms.enums.PayType;
import com.hrms.enums.PayrollStatus;
import com.hrms.enums.TimesheetStatus;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.CompanyRepository;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.PayrollRunRepository;
import com.hrms.repository.PayslipRepository;
import com.hrms.repository.WeeklyTimesheetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollService {

    private static final BigDecimal FLAT_TAX_RATE = new BigDecimal("0.15");
    private static final BigDecimal SOCIAL_SECURITY_RATE = new BigDecimal("0.062");

    private final PayrollRunRepository payrollRunRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final WeeklyTimesheetRepository weeklyTimesheetRepository;
    private final PdfService pdfService;

    public PayrollPreviewResponse previewPayroll(Long companyId, PayrollRunRequest request) {
        boolean alreadyProcessed = payrollRunRepository
                .findByCompanyIdAndPeriodMonthAndPeriodYear(companyId, request.getPeriodMonth(), request.getPeriodYear())
                .isPresent();

        List<Employee> activeEmployees = employeeRepository.findByCompanyId(companyId).stream()
                .filter(e -> e.getUser() != null && Boolean.TRUE.equals(e.getUser().getIsActive()))
                .collect(Collectors.toList());

        Map<Long, PayrollAdjustmentRequest> adjustmentsByEmployee = indexAdjustments(request.getAdjustments());

        List<PayslipResponse> lines = activeEmployees.stream()
                .map(employee -> calculate(employee, request.getPeriodMonth(), request.getPeriodYear(),
                        adjustmentsByEmployee.get(employee.getId()), null))
                .collect(Collectors.toList());

        BigDecimal totalGross = lines.stream().map(PayslipResponse::getGrossSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNet = lines.stream().map(PayslipResponse::getNetSalary).reduce(BigDecimal.ZERO, BigDecimal::add);

        return PayrollPreviewResponse.builder()
                .periodMonth(request.getPeriodMonth())
                .periodYear(request.getPeriodYear())
                .alreadyProcessed(alreadyProcessed)
                .lines(lines)
                .totalGross(totalGross)
                .totalNet(totalNet)
                .employeeCount(lines.size())
                .build();
    }

    @Transactional
    public List<PayslipResponse> runPayroll(Long companyId, Long processedByUserId, PayrollRunRequest request) {
        if (payrollRunRepository.findByCompanyIdAndPeriodMonthAndPeriodYear(companyId, request.getPeriodMonth(), request.getPeriodYear()).isPresent()) {
            throw new BadRequestException("Payroll for this period has already been processed");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        Employee processedBy = employeeRepository.findByUserId(processedByUserId).orElse(null);

        PayrollRun run = PayrollRun.builder()
                .company(company)
                .periodMonth(request.getPeriodMonth())
                .periodYear(request.getPeriodYear())
                .status(PayrollStatus.PAID)
                .processedBy(processedBy)
                .processedAt(LocalDateTime.now())
                .build();
        run = payrollRunRepository.save(run);

        List<Employee> activeEmployees = employeeRepository.findByCompanyId(companyId).stream()
                .filter(e -> e.getUser() != null && Boolean.TRUE.equals(e.getUser().getIsActive()))
                .collect(Collectors.toList());

        Map<Long, PayrollAdjustmentRequest> adjustmentsByEmployee = indexAdjustments(request.getAdjustments());

        final PayrollRun savedRun = run;
        return activeEmployees.stream()
                .map(employee -> {
                    PayslipResponse calculated = calculate(employee, request.getPeriodMonth(), request.getPeriodYear(),
                            adjustmentsByEmployee.get(employee.getId()), savedRun);
                    return persistAndGeneratePdf(calculated, employee, savedRun, request.getPeriodMonth(), request.getPeriodYear());
                })
                .collect(Collectors.toList());
    }

    private Map<Long, PayrollAdjustmentRequest> indexAdjustments(List<PayrollAdjustmentRequest> adjustments) {
        Map<Long, PayrollAdjustmentRequest> map = new HashMap<>();
        if (adjustments != null) {
            for (PayrollAdjustmentRequest a : adjustments) {
                if (a.getEmployeeId() != null) {
                    map.put(a.getEmployeeId(), a);
                }
            }
        }
        return map;
    }

    private PayslipResponse calculate(Employee employee, int periodMonth, int periodYear,
                                       PayrollAdjustmentRequest adjustment, PayrollRun run) {
        Double hoursWorked = null;
        BigDecimal gross;

        if (employee.getPayType() == PayType.HOURLY) {
            YearMonth ym = YearMonth.of(periodYear, periodMonth);
            LocalDate periodStart = ym.atDay(1);
            LocalDate periodEnd = ym.atEndOfMonth();

            List<com.hrms.entity.WeeklyTimesheet> approvedTimesheets = weeklyTimesheetRepository
                    .findByEmployeeIdAndStatusAndWeekStartDateBetween(employee.getId(), TimesheetStatus.APPROVED, periodStart, periodEnd);

            hoursWorked = approvedTimesheets.stream().mapToDouble(com.hrms.entity.WeeklyTimesheet::getTotalHours).sum();
            BigDecimal rate = employee.getHourlyRate() != null ? employee.getHourlyRate() : BigDecimal.ZERO;
            gross = rate.multiply(BigDecimal.valueOf(hoursWorked)).setScale(2, RoundingMode.HALF_UP);
        } else {
            gross = employee.getSalaryAmount()
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        }

        BigDecimal bonus = adjustment != null && adjustment.getBonusAmount() != null ? adjustment.getBonusAmount() : BigDecimal.ZERO;
        BigDecimal extraDeduction = adjustment != null && adjustment.getDeductionAmount() != null ? adjustment.getDeductionAmount() : BigDecimal.ZERO;

        BigDecimal taxableGross = gross.add(bonus);
        BigDecimal tax = taxableGross.multiply(FLAT_TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal socialSecurity = taxableGross.multiply(SOCIAL_SECURITY_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = taxableGross.subtract(tax).subtract(socialSecurity).subtract(extraDeduction);

        return PayslipResponse.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .periodMonth(periodMonth)
                .periodYear(periodYear)
                .payType(employee.getPayType())
                .hoursWorked(hoursWorked)
                .grossSalary(taxableGross)
                .taxDeduction(tax)
                .socialSecurityDeduction(socialSecurity)
                .bonusAmount(bonus)
                .otherDeductions(extraDeduction)
                .netSalary(net)
                .build();
    }

    private PayslipResponse persistAndGeneratePdf(PayslipResponse calculated, Employee employee, PayrollRun run, int month, int year) {
        Payslip payslip = Payslip.builder()
                .payrollRun(run)
                .employee(employee)
                .grossSalary(calculated.getGrossSalary())
                .taxDeduction(calculated.getTaxDeduction())
                .socialSecurityDeduction(calculated.getSocialSecurityDeduction())
                .bonusAmount(calculated.getBonusAmount())
                .hoursWorked(calculated.getHoursWorked())
                .otherDeductions(calculated.getOtherDeductions())
                .netSalary(calculated.getNetSalary())
                .build();
        payslip = payslipRepository.save(payslip);

        try {
            String pdfPath = pdfService.generatePayslipPdf(payslip, month, year);
            payslip.setPdfPath(pdfPath);
            payslip = payslipRepository.save(payslip);
        } catch (Exception ex) {
            log.error("Failed to generate payslip PDF for employee {}: {}", employee.getId(), ex.getMessage());
        }

        calculated.setId(payslip.getId());
        calculated.setDownloadUrl("/payroll/payslips/" + payslip.getId() + "/download");
        return calculated;
    }

    public List<PayslipResponse> getMyPayslips(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        return payslipRepository.findByEmployeeIdOrderByIdDesc(employee.getId()).stream()
                .map(p -> toStoredResponse(p, employee))
                .collect(Collectors.toList());
    }

    public Payslip getPayslipForDownload(Long companyId, Long payslipId, Long requestingUserId, boolean isPrivileged) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found"));

        if (!payslip.getEmployee().getCompany().getId().equals(companyId)) {
            throw new ResourceNotFoundException("Payslip not found");
        }

        if (!isPrivileged) {
            Employee employee = employeeRepository.findByUserId(requestingUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
            if (!payslip.getEmployee().getId().equals(employee.getId())) {
                throw new BadRequestException("You do not have access to this payslip");
            }
        }
        return payslip;
    }

    private PayslipResponse toStoredResponse(Payslip p, Employee employee) {
        return PayslipResponse.builder()
                .id(p.getId())
                .employeeId(employee.getId())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .periodMonth(p.getPayrollRun().getPeriodMonth())
                .periodYear(p.getPayrollRun().getPeriodYear())
                .payType(employee.getPayType())
                .hoursWorked(p.getHoursWorked())
                .grossSalary(p.getGrossSalary())
                .taxDeduction(p.getTaxDeduction())
                .socialSecurityDeduction(p.getSocialSecurityDeduction())
                .bonusAmount(p.getBonusAmount())
                .otherDeductions(p.getOtherDeductions())
                .netSalary(p.getNetSalary())
                .downloadUrl("/payroll/payslips/" + p.getId() + "/download")
                .build();
    }
}
