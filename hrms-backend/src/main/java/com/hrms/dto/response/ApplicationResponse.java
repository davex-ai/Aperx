package com.hrms.dto.response;

import com.hrms.enums.ApplicationStatus;
import com.hrms.enums.EducationLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String resumeFileName;
    private String resumePreviewUrl;
    private String whyJoin;
    private String availability;
    private Double yearsOfExperience;
    private EducationLevel highestEducation;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
    private List<ApplicationAnswerResponse> answers;
}
