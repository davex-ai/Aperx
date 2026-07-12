package com.hrms.dto.request;

import com.hrms.enums.ReportCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnonymousReportRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String body;

    @NotNull
    private ReportCategory category;
}
