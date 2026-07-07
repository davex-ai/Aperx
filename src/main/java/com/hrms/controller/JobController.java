package com.hrms.controller;

import com.hrms.dto.request.ApplicationStatusUpdateRequest;
import com.hrms.dto.request.JobPostingRequest;
import com.hrms.dto.response.ApplicationResponse;
import com.hrms.dto.response.JobPostingResponse;
import com.hrms.service.RecruitmentService;
import com.hrms.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class JobController {

    private final RecruitmentService recruitmentService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<List<JobPostingResponse>> getAll() {
        return ResponseEntity.ok(recruitmentService.getAllJobs());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobPostingResponse> create(@Valid @RequestBody JobPostingRequest request) {
        return ResponseEntity.ok(recruitmentService.createJobPosting(authUtil.getCurrentUserId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobPostingResponse> update(@PathVariable Long id, @Valid @RequestBody JobPostingRequest request) {
        return ResponseEntity.ok(recruitmentService.updateJobPosting(id, request));
    }

    @GetMapping("/{id}/applications")
    public ResponseEntity<List<ApplicationResponse>> getApplications(@PathVariable Long id) {
        return ResponseEntity.ok(recruitmentService.getApplicationsForJob(id));
    }

    @PutMapping("/applications/{applicationId}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(@PathVariable Long applicationId, @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        return ResponseEntity.ok(recruitmentService.updateApplicationStatus(applicationId, request));
    }
}
