package com.hrms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobQuestionResponse {
    private Long id;
    private String questionText;
    private Boolean isRequired;
    private Integer displayOrder;
}
