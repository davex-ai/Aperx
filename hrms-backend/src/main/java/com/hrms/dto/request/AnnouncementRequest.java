package com.hrms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnnouncementRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String body;

    private Boolean isPinned;
}
