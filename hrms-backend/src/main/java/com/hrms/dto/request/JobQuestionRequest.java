package com.hrms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobQuestionRequest {
    @NotBlank
    private String questionText;
    private Boolean isRequired;
}
