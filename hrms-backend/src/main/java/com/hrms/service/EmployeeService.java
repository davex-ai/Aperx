package com.hrms.service;

import com.hrms.dto.request.EmergencyContactRequest;
import com.hrms.dto.request.UpdateProfileRequest;
import com.hrms.dto.response.EmployeeResponse;
import com.hrms.entity.BankAccount;
import com.hrms.entity.EmergencyContact;
import com.hrms.entity.Employee;
import com.hrms.entity.User;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.BankAccountRepository;
import com.hrms.repository.EmergencyContactRepository;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.UserRepository;
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
    private final UserRepository userRepository;

    public EmployeeResponse getByUserId(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        return toResponse(employee, true);
    }

    public EmployeeResponse getById(Long companyId, Long employeeId, boolean includeSalary) {
        Employee employee = employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + employeeId));
        return toResponse(employee, includeSalary);
    }

    public EmployeeResponse getEmployeeForViewer(Long companyId, Long targetEmployeeId, Long viewerUserId) {
        Employee target = employeeRepository.findByIdAndCompanyId(targetEmployeeId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + targetEmployeeId));

        Employee viewer = employeeRepository.findByUserId(viewerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        boolean isAdmin = viewer.getUser().getRole() == com.hrms.enums.UserRole.ROLE_ADMIN;
        boolean isSelf = target.getId().equals(viewer.getId());
        boolean isDirectReport = target.getManager() != null && target.getManager().getId().equals(viewer.getId());

        boolean includeSalary = isAdmin || isSelf || isDirectReport;
        return toResponse(target, includeSalary);
    }

    public List<EmployeeResponse> getAllForCompany(Long companyId) {
        return employeeRepository.findByCompanyId(companyId).stream()
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

    @Transactional
    public void terminateEmployee(Long companyId, Long requestingUserId, Long employeeId) {
        Employee target = employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Employee requester = employeeRepository.findByUserId(requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Requesting employee profile not found"));

        if (target.getId().equals(requester.getId())) {
            throw new com.hrms.exception.BadRequestException("You cannot terminate your own account");
        }

        User user = target.getUser();
        user.setIsActive(false);
        userRepository.save(user);
    }

    private EmployeeResponse toResponse(Employee e, boolean includeSalary) {
        User user = e.getUser();
        String stage;
        if (user == null || user.getEmail() == null) {
            stage = "DRAFT";
        } else if (Boolean.TRUE.equals(user.getMustChangePassword())) {
            stage = "INVITED";
        } else {
            stage = "ACTIVE";
        }

        return EmployeeResponse.builder()
                .id(e.getId())
                .personalEmail(user != null ? user.getPersonalEmail() : null)
                .companyEmail(user != null ? user.getEmail() : null)
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .phoneNumber(e.getPhoneNumber())
                .department(e.getDepartment())
                .jobTitle(e.getJobTitle())
                .salaryGrade(e.getSalaryGrade())
                .hireDate(e.getHireDate())
                .salaryAmount(includeSalary ? e.getSalaryAmount() : null)
                .payType(e.getPayType())
                .hourlyRate(includeSalary ? e.getHourlyRate() : null)
                .role(user != null ? user.getRole().name() : null)
                .managerId(e.getManager() != null ? e.getManager().getId() : null)
                .managerName(e.getManager() != null ? e.getManager().getFirstName() + " " + e.getManager().getLastName() : null)
                .active(user != null && Boolean.TRUE.equals(user.getIsActive()))
                .onboardingStage(stage)
                .build();
    }
}
