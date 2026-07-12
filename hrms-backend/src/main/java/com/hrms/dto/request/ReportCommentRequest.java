package com.hrms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReportCommentRequest {
    @NotBlank
    private String body;
}
