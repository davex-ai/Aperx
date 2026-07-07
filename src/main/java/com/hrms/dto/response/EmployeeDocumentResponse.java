package com.hrms.dto.response;

import com.hrms.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDocumentResponse {
    private Long id;
    private DocumentType type;
    private String title;
    private String downloadUrl;
    private LocalDateTime createdAt;
}
