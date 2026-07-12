package com.hrms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.dto.response.ApplicationResponse;
import com.hrms.dto.response.JobPostingResponse;
import com.hrms.enums.EducationLevel;
import com.hrms.exception.BadRequestException;
import com.hrms.service.RecruitmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/careers")
@RequiredArgsConstructor
public class CareersController {

    private final RecruitmentService recruitmentService;
    private final ObjectMapper objectMapper;

    @GetMapping("/{companySlug}/jobs")
    public ResponseEntity<List<JobPostingResponse>> getOpenJobs(@PathVariable String companySlug) {
        return ResponseEntity.ok(recruitmentService.getOpenJobsForCompanySlug(companySlug));
    }

    @GetMapping("/{companySlug}/jobs/{jobId}")
    public ResponseEntity<JobPostingResponse> getOpenJob(@PathVariable String companySlug, @PathVariable Long jobId) {
        return ResponseEntity.ok(recruitmentService.getOpenJobForCompanySlug(companySlug, jobId));
    }

    @PostMapping(value = "/{companySlug}/jobs/{jobId}/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApplicationResponse> apply(
            @PathVariable String companySlug,
            @PathVariable Long jobId,
            @RequestParam String candidateName,
            @RequestParam String candidateEmail,
            @RequestParam String candidatePhone,
            @RequestParam String whyJoin,
            @RequestParam String availability,
            @RequestParam Double yearsOfExperience,
            @RequestParam EducationLevel highestEducation,
            @RequestParam(value = "answersJson", required = false) String answersJson,
            @RequestParam("resume") MultipartFile resume) {

        Map<Long, String> answers = new HashMap<>();
        if (answersJson != null && !answersJson.isBlank()) {
            try {
                Map<String, String> raw = objectMapper.readValue(answersJson, Map.class);
                raw.forEach((k, v) -> answers.put(Long.valueOf(k), v));
            } catch (Exception e) {
                throw new BadRequestException("Invalid answers format");
            }
        }

        ApplicationResponse response = recruitmentService.submitApplication(
                companySlug, jobId, candidateName, candidateEmail, candidatePhone,
                whyJoin, availability, yearsOfExperience, highestEducation, answers, resume);

        return ResponseEntity.ok(response);
    }
}
