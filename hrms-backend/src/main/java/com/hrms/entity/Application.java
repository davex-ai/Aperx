package com.hrms.entity;

import com.hrms.enums.ApplicationStatus;
import com.hrms.enums.EducationLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobPosting job;

    @Column(name = "candidate_name", nullable = false, length = 100)
    private String candidateName;

    @Column(name = "candidate_email", nullable = false, length = 150)
    private String candidateEmail;

    @Column(name = "candidate_phone", length = 30)
    private String candidatePhone;

    @Column(name = "resume_file_path", nullable = false)
    private String resumeFilePath;

    @Column(name = "resume_file_name")
    private String resumeFileName;

    @Column(name = "why_join", columnDefinition = "TEXT")
    private String whyJoin;

    @Column(name = "availability", length = 100)
    private String availability;

    @Column(name = "years_of_experience")
    private Double yearsOfExperience;

    @Enumerated(EnumType.STRING)
    @Column(name = "highest_education")
    private EducationLevel highestEducation;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "applied_at", updatable = false)
    private LocalDateTime appliedAt;

    @PrePersist
    protected void onCreate() {
        this.appliedAt = LocalDateTime.now();
    }
}
