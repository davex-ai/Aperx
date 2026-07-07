package com.hrms.service;

import com.hrms.dto.request.LeaveRequestCreate;
import com.hrms.dto.request.LeaveReviewRequest;
import com.hrms.dto.response.LeaveBalanceResponse;
import com.hrms.dto.response.LeaveRequestResponse;
import com.hrms.entity.Employee;
import com.hrms.entity.LeaveBalance;
import com.hrms.entity.LeaveRequest;
import com.hrms.enums.LeaveStatus;
import com.hrms.enums.LeaveType;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.LeaveBalanceRepository;
import com.hrms.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final EmailService emailService;

    private static final double[] DEFAULT_ANNUAL_ALLOWANCE = {20, 10, 90, 5};

    @Transactional
    public LeaveRequestResponse submitRequest(Long userId, LeaveRequestCreate request) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }
        if (request.getType() == LeaveType.SICK
                && ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) >= 2
                && (request.getCertificateUrl() == null || request.getCertificateUrl().isBlank())) {
            throw new BadRequestException("A medical certificate is required for sick leave of 3 or more days");
        }

        double requestedDays = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        LeaveBalance balance = getOrCreateBalance(employee, request.getType());
        double remaining = balance.getTotalDays() - balance.getUsedDays();
        if (requestedDays > remaining) {
            throw new BadRequestException(
                    String.format("Insufficient %s leave balance: requested %.1f day(s), %.1f remaining",
                            request.getType(), requestedDays, remaining));
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .type(request.getType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .certificateUrl(request.getCertificateUrl())
                .status(LeaveStatus.PENDING)
                .build();
        leaveRequest = leaveRequestRepository.save(leaveRequest);

        return toResponse(leaveRequest);
    }

    @Transactional
    public LeaveRequestResponse reviewRequest(Long reviewerUserId, Long leaveRequestId, LeaveReviewRequest request) {
        if (request.getStatus() != LeaveStatus.APPROVED && request.getStatus() != LeaveStatus.REJECTED) {
            throw new BadRequestException("Review status must be either APPROVED or REJECTED");
        }

        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("This request has already been reviewed");
        }

        Employee reviewer = employeeRepository.findByUserId(reviewerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer profile not found"));

        leaveRequest.setStatus(request.getStatus());
        leaveRequest.setReviewComment(request.getReviewComment());
        leaveRequest.setApprovedBy(reviewer);
        leaveRequest = leaveRequestRepository.save(leaveRequest);

        if (request.getStatus() == LeaveStatus.APPROVED) {
            double days = ChronoUnit.DAYS.between(leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1;
            LeaveBalance balance = getOrCreateBalance(leaveRequest.getEmployee(), leaveRequest.getType());
            balance.setUsedDays(balance.getUsedDays() + days);
            leaveBalanceRepository.save(balance);
        }

        Employee employee = leaveRequest.getEmployee();
        if (employee.getUser() != null) {
            emailService.sendLeaveDecisionEmail(
                    employee.getUser().getEmail(),
                    employee.getFirstName(),
                    request.getStatus().name(),
                    request.getReviewComment());
        }

        return toResponse(leaveRequest);
    }

    public List<LeaveRequestResponse> getMyRequests(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        return leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<LeaveRequestResponse> getPendingForManager(Long managerId) {
        return leaveRequestRepository.findByEmployeeManagerIdAndStatus(managerId, LeaveStatus.PENDING).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<LeaveRequestResponse> getAllPending() {
        return leaveRequestRepository.findByStatusOrderByCreatedAtDesc(LeaveStatus.PENDING).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<LeaveBalanceResponse> getMyBalances(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        LeaveType[] types = LeaveType.values();
        return List.of(types).stream()
                .map(type -> {
                    LeaveBalance balance = getOrCreateBalance(employee, type);
                    return LeaveBalanceResponse.builder()
                            .type(type)
                            .totalDays(balance.getTotalDays())
                            .usedDays(balance.getUsedDays())
                            .remainingDays(balance.getTotalDays() - balance.getUsedDays())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private LeaveBalance getOrCreateBalance(Employee employee, LeaveType type) {
        int year = LocalDate.now().getYear();
        return leaveBalanceRepository.findByEmployeeIdAndTypeAndYear(employee.getId(), type, year)
                .orElseGet(() -> leaveBalanceRepository.save(
                        LeaveBalance.builder()
                                .employee(employee)
                                .type(type)
                                .year(year)
                                .totalDays(DEFAULT_ANNUAL_ALLOWANCE[type.ordinal()])
                                .usedDays(0.0)
                                .build()
                ));
    }

    private LeaveRequestResponse toResponse(LeaveRequest lr) {
        Employee e = lr.getEmployee();
        Employee approver = lr.getApprovedBy();
        return LeaveRequestResponse.builder()
                .id(lr.getId())
                .employeeId(e.getId())
                .employeeName(e.getFirstName() + " " + e.getLastName())
                .type(lr.getType())
                .startDate(lr.getStartDate())
                .endDate(lr.getEndDate())
                .reason(lr.getReason())
                .certificateUrl(lr.getCertificateUrl())
                .status(lr.getStatus())
                .reviewComment(lr.getReviewComment())
                .approvedByName(approver != null ? approver.getFirstName() + " " + approver.getLastName() : null)
                .createdAt(lr.getCreatedAt())
                .build();
    }
}
