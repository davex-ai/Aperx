package com.hrms.dto.response;

import com.hrms.enums.JobStatus;
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
public class JobPostingResponse {
    private Long id;
    private String title;
    private String description;
    private String department;
    private JobStatus status;
    private long applicantCount;
    private LocalDateTime createdAt;
    private List<JobQuestionResponse> questions;
}
