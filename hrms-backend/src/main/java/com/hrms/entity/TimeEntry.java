package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "time_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "clock_in_at", nullable = false)
    private LocalDateTime clockInAt;

    @Column(name = "clock_in_lat")
    private Double clockInLat;

    @Column(name = "clock_in_lng")
    private Double clockInLng;

    @Column(name = "clock_out_at")
    private LocalDateTime clockOutAt;

    @Column(name = "clock_out_lat")
    private Double clockOutLat;

    @Column(name = "clock_out_lng")
    private Double clockOutLng;

    @Column(length = 255)
    private String notes;

    public double durationHours() {
        if (clockOutAt == null) return 0.0;
        return java.time.Duration.between(clockInAt, clockOutAt).toMinutes() / 60.0;
    }
}
