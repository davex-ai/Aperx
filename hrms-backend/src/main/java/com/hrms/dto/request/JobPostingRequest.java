package com.hrms.dto.request;

import com.hrms.enums.JobStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class JobPostingRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String description;
    @NotBlank
    private String department;
    private JobStatus status;
    private List<JobQuestionRequest> questions;
}
