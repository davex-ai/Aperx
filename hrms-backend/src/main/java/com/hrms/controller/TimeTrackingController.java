package com.hrms.controller;

import com.hrms.dto.request.ClockInRequest;
import com.hrms.dto.request.ClockOutRequest;
import com.hrms.dto.request.TimesheetReviewRequest;
import com.hrms.dto.response.TimeEntryResponse;
import com.hrms.dto.response.WeeklyTimesheetResponse;
import com.hrms.service.TimeTrackingService;
import com.hrms.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/time-tracking")
@RequiredArgsConstructor
public class TimeTrackingController {

    private final TimeTrackingService timeTrackingService;
    private final AuthUtil authUtil;

    @PostMapping("/clock-in")
    public ResponseEntity<TimeEntryResponse> clockIn(@RequestBody ClockInRequest request) {
        return ResponseEntity.ok(timeTrackingService.clockIn(authUtil.getCurrentUserId(), request));
    }

    @PostMapping("/clock-out")
    public ResponseEntity<TimeEntryResponse> clockOut(@RequestBody ClockOutRequest request) {
        return ResponseEntity.ok(timeTrackingService.clockOut(authUtil.getCurrentUserId(), request));
    }

    @GetMapping("/active")
    public ResponseEntity<TimeEntryResponse> getActive() {
        return ResponseEntity.ok(timeTrackingService.getActiveEntry(authUtil.getCurrentUserId()));
    }

    @GetMapping("/entries/me")
    public ResponseEntity<List<TimeEntryResponse>> getMyEntries() {
        return ResponseEntity.ok(timeTrackingService.getRecentEntries(authUtil.getCurrentUserId()));
    }

    @PostMapping("/timesheets/submit-current-week")
    public ResponseEntity<WeeklyTimesheetResponse> submitCurrentWeek() {
        return ResponseEntity.ok(timeTrackingService.submitCurrentWeek(authUtil.getCurrentUserId()));
    }

    @GetMapping("/timesheets/me")
    public ResponseEntity<List<WeeklyTimesheetResponse>> getMyTimesheets() {
        return ResponseEntity.ok(timeTrackingService.getMyTimesheets(authUtil.getCurrentUserId()));
    }

    @GetMapping("/timesheets/pending")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<WeeklyTimesheetResponse>> getPending() {
        boolean isAdmin = "ROLE_ADMIN".equals(authUtil.getCurrentUser().getRole().name());
        return ResponseEntity.ok(timeTrackingService.getPendingForReview(authUtil.getCurrentCompanyId(), authUtil.getCurrentUserId(), isAdmin));
    }

    @PutMapping("/timesheets/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<WeeklyTimesheetResponse> review(@PathVariable Long id, @Valid @RequestBody TimesheetReviewRequest request) {
        return ResponseEntity.ok(timeTrackingService.reviewTimesheet(authUtil.getCurrentUserId(), id, request));
    }
}
