package com.hrms.service;

import com.hrms.dto.request.ApplicationCreateRequest;
import com.hrms.dto.request.ApplicationStatusUpdateRequest;
import com.hrms.dto.request.JobPostingRequest;
import com.hrms.dto.response.ApplicationResponse;
import com.hrms.dto.response.JobPostingResponse;
import com.hrms.entity.Application;
import com.hrms.entity.Employee;
import com.hrms.entity.JobPosting;
import com.hrms.enums.JobStatus;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.ApplicationRepository;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecruitmentService {

    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public JobPostingResponse createJobPosting(Long userId, JobPostingRequest request) {
        Employee poster = employeeRepository.findByUserId(userId).orElse(null);

        JobPosting job = JobPosting.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .department(request.getDepartment())
                .status(request.getStatus() != null ? request.getStatus() : JobStatus.OPEN)
                .postedBy(poster)
                .build();
        job = jobPostingRepository.save(job);
        return toResponse(job);
    }

    @Transactional
    public JobPostingResponse updateJobPosting(Long jobId, JobPostingRequest request) {
        JobPosting job = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found"));
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setDepartment(request.getDepartment());
        if (request.getStatus() != null) job.setStatus(request.getStatus());
        job = jobPostingRepository.save(job);
        return toResponse(job);
    }

    public List<JobPostingResponse> getOpenJobs() {
        return jobPostingRepository.findByStatus(JobStatus.OPEN).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<JobPostingResponse> getAllJobs() {
        return jobPostingRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApplicationResponse submitApplication(Long jobId, ApplicationCreateRequest request) {
        JobPosting job = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found"));

        if (job.getStatus() != JobStatus.OPEN) {
            throw new BadRequestException("This position is no longer accepting applications");
        }

        Application application = Application.builder()
                .job(job)
                .candidateName(request.getCandidateName())
                .candidateEmail(request.getCandidateEmail())
                .resumeUrl(request.getResumeUrl())
                .build();
        application = applicationRepository.save(application);
        return toResponse(application);
    }

    public List<ApplicationResponse> getApplicationsForJob(Long jobId) {
        return applicationRepository.findByJobId(jobId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(Long applicationId, ApplicationStatusUpdateRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        application.setStatus(request.getStatus());
        application = applicationRepository.save(application);
        return toResponse(application);
    }

    private JobPostingResponse toResponse(JobPosting job) {
        return JobPostingResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .department(job.getDepartment())
                .status(job.getStatus())
                .applicantCount(applicationRepository.countByJobId(job.getId()))
                .createdAt(job.getCreatedAt())
                .build();
    }

    private ApplicationResponse toResponse(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .jobId(app.getJob().getId())
                .jobTitle(app.getJob().getTitle())
                .candidateName(app.getCandidateName())
                .candidateEmail(app.getCandidateEmail())
                .resumeUrl(app.getResumeUrl())
                .status(app.getStatus())
                .appliedAt(app.getAppliedAt())
                .build();
    }
}
