package com.hrms.dto.response;

import com.hrms.enums.ReportCategory;
import com.hrms.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnonymousReportResponse {
    private Long id;
    private String displayHandle;
    private String title;
    private String body;
    private ReportCategory category;
    private ReportStatus status;
    private long commentCount;
    private LocalDateTime createdAt;
    private boolean isMine;
}
