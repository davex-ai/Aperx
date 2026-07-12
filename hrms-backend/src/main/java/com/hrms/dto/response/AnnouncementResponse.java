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
public class AnnouncementResponse {
    private Long id;
    private String title;
    private String body;
    private Boolean isPinned;
    private Long authorId;
    private String authorName;
    private String authorJobTitle;
    private long commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean canManage;
}
