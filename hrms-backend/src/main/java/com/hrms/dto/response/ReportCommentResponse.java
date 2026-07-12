package com.hrms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCommentResponse {
    private Long id;
    private String displayName;
    private boolean isReporter;
    private boolean isMine;
    private String body;
    private LocalDateTime createdAt;
}
