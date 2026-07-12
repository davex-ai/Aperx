package com.hrms.controller;

import com.hrms.dto.request.AssignCompanyEmailRequest;
import com.hrms.dto.request.CreateEmployeeRequest;
import com.hrms.dto.request.EmergencyContactRequest;
import com.hrms.dto.request.UpdateProfileRequest;
import com.hrms.dto.response.EmployeeResponse;
import com.hrms.entity.Employee;
import com.hrms.service.AuthService;
import com.hrms.service.EmployeeService;
import com.hrms.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final AuthService authService;
    private final AuthUtil authUtil;

    @GetMapping("/api/profile/me")
    public ResponseEntity<EmployeeResponse> getMyProfile() {
        return ResponseEntity.ok(employeeService.getByUserId(authUtil.getCurrentUserId()));
    }

    @PutMapping("/api/profile/me")
    public ResponseEntity<EmployeeResponse> updateMyProfile(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(employeeService.updateOwnProfile(authUtil.getCurrentUserId(), request));
    }

    @PostMapping("/api/profile/me/emergency-contacts")
    public ResponseEntity<Void> addEmergencyContact(@Valid @RequestBody EmergencyContactRequest request) {
        employeeService.addEmergencyContact(authUtil.getCurrentUserId(), request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/employees/{id}")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeForViewer(authUtil.getCurrentCompanyId(), id, authUtil.getCurrentUserId()));
    }

    @GetMapping("/api/manager/team")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<EmployeeResponse>> getMyTeam() {
        Long employeeId = employeeService.getByUserId(authUtil.getCurrentUserId()).getId();
        return ResponseEntity.ok(employeeService.getDirectReports(employeeId));
    }

    @GetMapping("/api/admin/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllForCompany(authUtil.getCurrentCompanyId()));
    }

    @PostMapping("/api/admin/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        Long companyId = authUtil.getCurrentCompanyId();
        Employee created = authService.createDraftEmployee(companyId, request);
        return ResponseEntity.ok(employeeService.getById(companyId, created.getId(), true));
    }

    @PutMapping("/api/admin/employees/{id}/assign-company-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> assignCompanyEmail(@PathVariable Long id, @Valid @RequestBody AssignCompanyEmailRequest request) {
        Long companyId = authUtil.getCurrentCompanyId();
        boolean invitationSent = authService.assignCompanyEmail(companyId, id, request);
        EmployeeResponse response = employeeService.getById(companyId, id, true);
        response.setInvitationSent(invitationSent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/admin/employees/{id}/resend-invitation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> resendInvitation(@PathVariable Long id) {
        return ResponseEntity.ok(authService.resendInvitation(authUtil.getCurrentCompanyId(), id));
    }

    @PutMapping("/api/admin/employees/{id}/terminate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> terminateEmployee(@PathVariable Long id) {
        employeeService.terminateEmployee(authUtil.getCurrentCompanyId(), authUtil.getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
