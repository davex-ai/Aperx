package com.hrms.dto.request;

import com.hrms.enums.LeaveType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveRequestCreate {
    @NotNull
    private LeaveType type;

    @NotNull @FutureOrPresent
    private LocalDate startDate;

    @NotNull @FutureOrPresent
    private LocalDate endDate;

    private String reason;

    private String certificateUrl;
}
