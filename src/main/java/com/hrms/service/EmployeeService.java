package com.hrms.service;

import com.hrms.dto.request.EmergencyContactRequest;
import com.hrms.dto.request.UpdateProfileRequest;
import com.hrms.dto.response.EmployeeResponse;
import com.hrms.entity.BankAccount;
import com.hrms.entity.EmergencyContact;
import com.hrms.entity.Employee;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.BankAccountRepository;
import com.hrms.repository.EmergencyContactRepository;
import com.hrms.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final BankAccountRepository bankAccountRepository;
    private final EmergencyContactRepository emergencyContactRepository;

    public EmployeeResponse getByUserId(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        return toResponse(employee, true);
    }

    public EmployeeResponse getById(Long employeeId, boolean includeSalary) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + employeeId));
        return toResponse(employee, includeSalary);
    }

    public List<EmployeeResponse> getAll() {
        return employeeRepository.findAll().stream()
                .map(e -> toResponse(e, true))
                .collect(Collectors.toList());
    }

    public List<EmployeeResponse> getDirectReports(Long managerId) {
        return employeeRepository.findByManagerId(managerId).stream()
                .map(e -> toResponse(e, false))
                .collect(Collectors.toList());
    }

    @Transactional
    public EmployeeResponse updateOwnProfile(Long userId, UpdateProfileRequest request) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        if (request.getPhoneNumber() != null) {
            employee.setPhoneNumber(request.getPhoneNumber());
            employeeRepository.save(employee);
        }

        if (request.getBankName() != null || request.getAccountNumber() != null) {
            BankAccount bankAccount = bankAccountRepository.findByEmployeeId(employee.getId())
                    .orElse(BankAccount.builder().employee(employee).build());
            if (request.getBankName() != null) bankAccount.setBankName(request.getBankName());
            if (request.getAccountHolderName() != null) bankAccount.setAccountHolderName(request.getAccountHolderName());
            if (request.getAccountNumber() != null) bankAccount.setAccountNumber(request.getAccountNumber());
            if (request.getRoutingNumber() != null) bankAccount.setRoutingNumber(request.getRoutingNumber());
            bankAccountRepository.save(bankAccount);
        }

        return toResponse(employee, true);
    }

    @Transactional
    public void addEmergencyContact(Long userId, EmergencyContactRequest request) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        EmergencyContact contact = EmergencyContact.builder()
                .employee(employee)
                .contactName(request.getContactName())
                .relationship(request.getRelationship())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .build();
        emergencyContactRepository.save(contact);
    }

    public List<EmergencyContact> getEmergencyContacts(Long employeeId) {
        return emergencyContactRepository.findByEmployeeId(employeeId);
    }

    private EmployeeResponse toResponse(Employee e, boolean includeSalary) {
        return EmployeeResponse.builder()
                .id(e.getId())
                .email(e.getUser() != null ? e.getUser().getEmail() : null)
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .phoneNumber(e.getPhoneNumber())
                .department(e.getDepartment())
                .jobTitle(e.getJobTitle())
                .salaryGrade(e.getSalaryGrade())
                .hireDate(e.getHireDate())
                .salaryAmount(includeSalary ? e.getSalaryAmount() : null)
                .role(e.getUser() != null ? e.getUser().getRole().name() : null)
                .managerId(e.getManager() != null ? e.getManager().getId() : null)
                .managerName(e.getManager() != null ? e.getManager().getFirstName() + " " + e.getManager().getLastName() : null)
                .active(e.getUser() != null && Boolean.TRUE.equals(e.getUser().getIsActive()))
                .build();
    }
}
