package com.hrms.service;

import com.hrms.dto.request.ApplicationStatusUpdateRequest;
import com.hrms.dto.request.CreateEmployeeRequest;
import com.hrms.dto.request.ExtendOfferRequest;
import com.hrms.dto.request.JobPostingRequest;
import com.hrms.dto.request.JobQuestionRequest;
import com.hrms.dto.response.ApplicationAnswerResponse;
import com.hrms.dto.response.ApplicationResponse;
import com.hrms.dto.response.JobPostingResponse;
import com.hrms.dto.response.JobQuestionResponse;
import com.hrms.entity.*;
import com.hrms.enums.ApplicationStatus;
import com.hrms.enums.EducationLevel;
import com.hrms.enums.JobStatus;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecruitmentService {

    private final JobPostingRepository jobPostingRepository;
    private final JobQuestionRepository jobQuestionRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationAnswerRepository applicationAnswerRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final ResumeStorageService resumeStorageService;
    private final AuthService authService;

    @Transactional
    public JobPostingResponse createJobPosting(Long companyId, Long userId, JobPostingRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        Employee poster = employeeRepository.findByUserId(userId).orElse(null);

        JobPosting job = JobPosting.builder()
                .company(company)
                .title(request.getTitle())
                .description(request.getDescription())
                .department(request.getDepartment())
                .status(request.getStatus() != null ? request.getStatus() : JobStatus.OPEN)
                .postedBy(poster)
                .build();
        job = jobPostingRepository.save(job);

        saveQuestions(job, request.getQuestions());

        return toResponse(job);
    }

    @Transactional
    public JobPostingResponse updateJobPosting(Long companyId, Long jobId, JobPostingRequest request) {
        JobPosting job = jobPostingRepository.findByIdAndCompanyId(jobId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found"));
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setDepartment(request.getDepartment());
        if (request.getStatus() != null) job.setStatus(request.getStatus());
        job = jobPostingRepository.save(job);

        if (request.getQuestions() != null) {
            jobQuestionRepository.deleteByJobPostingId(job.getId());
            saveQuestions(job, request.getQuestions());
        }

        return toResponse(job);
    }

    private void saveQuestions(JobPosting job, List<JobQuestionRequest> questions) {
        if (questions == null) return;
        int order = 0;
        for (JobQuestionRequest q : questions) {
            JobQuestion question = JobQuestion.builder()
                    .jobPosting(job)
                    .questionText(q.getQuestionText())
                    .isRequired(q.getIsRequired() == null || q.getIsRequired())
                    .displayOrder(order++)
                    .build();
            jobQuestionRepository.save(question);
        }
    }

    public List<JobPostingResponse> getOpenJobsForCompanySlug(String companySlug) {
        Company company = companyRepository.findBySlug(companySlug)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        return jobPostingRepository.findByCompanyIdAndStatus(company.getId(), JobStatus.OPEN).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public JobPostingResponse getOpenJobForCompanySlug(String companySlug, Long jobId) {
        Company company = companyRepository.findBySlug(companySlug)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        JobPosting job = jobPostingRepository.findByIdAndCompanyId(jobId, company.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found"));
        if (job.getStatus() != JobStatus.OPEN) {
            throw new ResourceNotFoundException("Job posting not found");
        }
        return toResponse(job);
    }

    public List<JobPostingResponse> getAllJobs(Long companyId) {
        return jobPostingRepository.findByCompanyId(companyId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApplicationResponse submitApplication(
            String companySlug,
            Long jobId,
            String candidateName,
            String candidateEmail,
            String candidatePhone,
            String whyJoin,
            String availability,
            Double yearsOfExperience,
            EducationLevel highestEducation,
            Map<Long, String> answers,
            MultipartFile resumeFile) {

        Company company = companyRepository.findBySlug(companySlug)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        JobPosting job = jobPostingRepository.findByIdAndCompanyId(jobId, company.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found"));

        if (job.getStatus() != JobStatus.OPEN) {
            throw new BadRequestException("This position is no longer accepting applications");
        }

        List<JobQuestion> questions = jobQuestionRepository.findByJobPostingIdOrderByDisplayOrderAsc(jobId);
        for (JobQuestion q : questions) {
            if (Boolean.TRUE.equals(q.getIsRequired())) {
                String answer = answers != null ? answers.get(q.getId()) : null;
                if (answer == null || answer.isBlank()) {
                    throw new BadRequestException("Please answer: " + q.getQuestionText());
                }
            }
        }

        ResumeStorageService.StoredResume stored = resumeStorageService.store(resumeFile);

        Application application = Application.builder()
                .job(job)
                .candidateName(candidateName)
                .candidateEmail(candidateEmail)
                .candidatePhone(candidatePhone)
                .resumeFilePath(stored.filePath())
                .resumeFileName(stored.originalFileName())
                .whyJoin(whyJoin)
                .availability(availability)
                .yearsOfExperience(yearsOfExperience)
                .highestEducation(highestEducation)
                .build();
        application = applicationRepository.save(application);

        if (answers != null) {
            for (JobQuestion q : questions) {
                String answerText = answers.get(q.getId());
                if (answerText != null && !answerText.isBlank()) {
                    applicationAnswerRepository.save(ApplicationAnswer.builder()
                            .application(application)
                            .jobQuestion(q)
                            .answerText(answerText)
                            .build());
                }
            }
        }

        return toResponse(application);
    }

    public List<ApplicationResponse> getApplicationsForJob(Long companyId, Long jobId) {
        JobPosting job = jobPostingRepository.findByIdAndCompanyId(jobId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found"));
        return applicationRepository.findByJobId(job.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Application getApplicationForPreview(Long companyId, Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (!application.getJob().getCompany().getId().equals(companyId)) {
            throw new ResourceNotFoundException("Application not found");
        }
        return application;
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(Long companyId, Long applicationId, ApplicationStatusUpdateRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!application.getJob().getCompany().getId().equals(companyId)) {
            throw new ResourceNotFoundException("Application not found");
        }

        application.setStatus(request.getStatus());
        application = applicationRepository.save(application);
        return toResponse(application);
    }

    @Transactional
    public Employee extendOffer(Long companyId, Long applicationId, ExtendOfferRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!application.getJob().getCompany().getId().equals(companyId)) {
            throw new ResourceNotFoundException("Application not found");
        }
        if (application.getStatus() != ApplicationStatus.OFFERED) {
            throw new BadRequestException("An offer can only be extended once the candidate reaches the Offered stage");
        }

        String[] nameParts = application.getCandidateName().trim().split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        CreateEmployeeRequest draftRequest = new CreateEmployeeRequest();
        draftRequest.setPersonalEmail(application.getCandidateEmail());
        draftRequest.setFirstName(firstName);
        draftRequest.setLastName(lastName);
        draftRequest.setPhoneNumber(application.getCandidatePhone());
        draftRequest.setDepartment(request.getDepartment());
        draftRequest.setJobTitle(request.getJobTitle());
        draftRequest.setSalaryGrade(request.getSalaryGrade());
        draftRequest.setHireDate(request.getHireDate());
        draftRequest.setSalaryAmount(request.getSalaryAmount());
        draftRequest.setPayType(request.getPayType());
        draftRequest.setHourlyRate(request.getHourlyRate());
        draftRequest.setManagerId(request.getManagerId());
        draftRequest.setRole(request.getRole());

        Employee employee = authService.createDraftEmployee(companyId, draftRequest);

        application.setStatus(ApplicationStatus.HIRED);
        applicationRepository.save(application);

        return employee;
    }

    private JobPostingResponse toResponse(JobPosting job) {
        List<JobQuestionResponse> questions = jobQuestionRepository.findByJobPostingIdOrderByDisplayOrderAsc(job.getId())
                .stream()
                .map(q -> JobQuestionResponse.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .isRequired(q.getIsRequired())
                        .displayOrder(q.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());

        return JobPostingResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .department(job.getDepartment())
                .status(job.getStatus())
                .applicantCount(applicationRepository.countByJobId(job.getId()))
                .createdAt(job.getCreatedAt())
                .questions(questions)
                .build();
    }

    private ApplicationResponse toResponse(Application app) {
        List<ApplicationAnswerResponse> answers = applicationAnswerRepository
                .findByApplicationIdOrderByJobQuestionDisplayOrderAsc(app.getId()).stream()
                .map(a -> ApplicationAnswerResponse.builder()
                        .questionId(a.getJobQuestion().getId())
                        .questionText(a.getJobQuestion().getQuestionText())
                        .answerText(a.getAnswerText())
                        .build())
                .collect(Collectors.toList());

        return ApplicationResponse.builder()
                .id(app.getId())
                .jobId(app.getJob().getId())
                .jobTitle(app.getJob().getTitle())
                .candidateName(app.getCandidateName())
                .candidateEmail(app.getCandidateEmail())
                .candidatePhone(app.getCandidatePhone())
                .resumeFileName(app.getResumeFileName())
                .resumePreviewUrl("/api/jobs/applications/" + app.getId() + "/resume-preview")
                .whyJoin(app.getWhyJoin())
                .availability(app.getAvailability())
                .yearsOfExperience(app.getYearsOfExperience())
                .highestEducation(app.getHighestEducation())
                .status(app.getStatus())
                .appliedAt(app.getAppliedAt())
                .answers(answers)
                .build();
    }
}
