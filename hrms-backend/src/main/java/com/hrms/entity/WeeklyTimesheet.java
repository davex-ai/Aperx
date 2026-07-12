package com.hrms.entity;

import com.hrms.enums.TimesheetStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_timesheets", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "week_start_date"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyTimesheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "total_hours", nullable = false)
    @Builder.Default
    private Double totalHours = 0.0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TimesheetStatus status = TimesheetStatus.OPEN;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private Employee reviewedBy;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
