package com.hrms.controller;

import com.hrms.dto.request.ApplicationStatusUpdateRequest;
import com.hrms.dto.request.ExtendOfferRequest;
import com.hrms.dto.request.JobPostingRequest;
import com.hrms.dto.response.ApplicationResponse;
import com.hrms.dto.response.EmployeeResponse;
import com.hrms.dto.response.JobPostingResponse;
import com.hrms.entity.Application;
import com.hrms.exception.BadRequestException;
import com.hrms.service.DocxPreviewService;
import com.hrms.service.EmployeeService;
import com.hrms.service.RecruitmentService;
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
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class JobController {

    private final RecruitmentService recruitmentService;
    private final DocxPreviewService docxPreviewService;
    private final EmployeeService employeeService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<List<JobPostingResponse>> getAll() {
        return ResponseEntity.ok(recruitmentService.getAllJobs(authUtil.getCurrentCompanyId()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobPostingResponse> create(@Valid @RequestBody JobPostingRequest request) {
        return ResponseEntity.ok(recruitmentService.createJobPosting(authUtil.getCurrentCompanyId(), authUtil.getCurrentUserId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobPostingResponse> update(@PathVariable Long id, @Valid @RequestBody JobPostingRequest request) {
        return ResponseEntity.ok(recruitmentService.updateJobPosting(authUtil.getCurrentCompanyId(), id, request));
    }

    @GetMapping("/{id}/applications")
    public ResponseEntity<List<ApplicationResponse>> getApplications(@PathVariable Long id) {
        return ResponseEntity.ok(recruitmentService.getApplicationsForJob(authUtil.getCurrentCompanyId(), id));
    }

    @PutMapping("/applications/{applicationId}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(@PathVariable Long applicationId, @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        return ResponseEntity.ok(recruitmentService.updateApplicationStatus(authUtil.getCurrentCompanyId(), applicationId, request));
    }

    @PostMapping("/applications/{applicationId}/extend-offer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> extendOffer(@PathVariable Long applicationId, @Valid @RequestBody ExtendOfferRequest request) {
        Long companyId = authUtil.getCurrentCompanyId();
        var employee = recruitmentService.extendOffer(companyId, applicationId, request);
        return ResponseEntity.ok(employeeService.getById(companyId, employee.getId(), true));
    }

    @GetMapping("/applications/{applicationId}/resume-preview")
    public ResponseEntity<?> previewResume(@PathVariable Long applicationId) {
        Application application = recruitmentService.getApplicationForPreview(authUtil.getCurrentCompanyId(), applicationId);
        String path = application.getResumeFilePath();

        if (path.endsWith(".pdf")) {
            File file = new File(path);
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + application.getResumeFileName() + "\"")
                    .body(resource);
        }

        if (path.endsWith(".docx")) {
            String html = docxPreviewService.convertToHtml(path);
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        }

        throw new BadRequestException("Unsupported resume file type");
    }
}
