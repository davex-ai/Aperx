package com.hrms.controller;

import com.hrms.dto.request.ApplicationCreateRequest;
import com.hrms.dto.response.ApplicationResponse;
import com.hrms.dto.response.JobPostingResponse;
import com.hrms.service.RecruitmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/careers")
@RequiredArgsConstructor
public class CareersController {

    private final RecruitmentService recruitmentService;

    @GetMapping("/jobs")
    public ResponseEntity<List<JobPostingResponse>> getOpenJobs() {
        return ResponseEntity.ok(recruitmentService.getOpenJobs());
    }

    @PostMapping("/jobs/{jobId}/apply")
    public ResponseEntity<ApplicationResponse> apply(@PathVariable Long jobId, @Valid @RequestBody ApplicationCreateRequest request) {
        return ResponseEntity.ok(recruitmentService.submitApplication(jobId, request));
    }
}
