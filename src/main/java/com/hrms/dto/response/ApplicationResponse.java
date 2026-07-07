package com.hrms.dto.response;

import com.hrms.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private String resumeUrl;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
}
