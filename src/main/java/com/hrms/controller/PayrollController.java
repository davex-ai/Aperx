package com.hrms.controller;

import com.hrms.dto.request.PayrollRunRequest;
import com.hrms.dto.response.PayslipResponse;
import com.hrms.entity.Payslip;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.service.PayrollService;
import com.hrms.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final AuthUtil authUtil;

    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PayslipResponse>> runPayroll(@Valid @RequestBody PayrollRunRequest request) {
        return ResponseEntity.ok(payrollService.runPayroll(authUtil.getCurrentUserId(), request));
    }

    @GetMapping("/payslips/me")
    public ResponseEntity<List<PayslipResponse>> myPayslips() {
        return ResponseEntity.ok(payrollService.getMyPayslips(authUtil.getCurrentUserId()));
    }

    @GetMapping("/payslips/{id}/download")
    public ResponseEntity<Resource> downloadPayslip(@PathVariable Long id) {
        boolean privileged = authUtil.isPrivileged();
        Payslip payslip = payrollService.getPayslipForDownload(id, authUtil.getCurrentUserId(), privileged);

        if (payslip.getPdfPath() == null) {
            throw new ResourceNotFoundException("Payslip PDF is not yet available");
        }

        File file = new File(payslip.getPdfPath());
        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
}
