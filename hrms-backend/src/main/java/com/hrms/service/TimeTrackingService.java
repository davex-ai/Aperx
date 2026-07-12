package com.hrms.service;

import com.hrms.dto.request.ClockInRequest;
import com.hrms.dto.request.ClockOutRequest;
import com.hrms.dto.request.TimesheetReviewRequest;
import com.hrms.dto.response.TimeEntryResponse;
import com.hrms.dto.response.WeeklyTimesheetResponse;
import com.hrms.entity.Employee;
import com.hrms.entity.TimeEntry;
import com.hrms.entity.WeeklyTimesheet;
import com.hrms.enums.TimesheetStatus;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.TimeEntryRepository;
import com.hrms.repository.WeeklyTimesheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeTrackingService {

    private final TimeEntryRepository timeEntryRepository;
    private final WeeklyTimesheetRepository weeklyTimesheetRepository;
    private final EmployeeRepository employeeRepository;

    private static LocalDate weekStartOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    @Transactional
    public TimeEntryResponse clockIn(Long userId, ClockInRequest request) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        timeEntryRepository.findFirstByEmployeeIdAndClockOutAtIsNullOrderByClockInAtDesc(employee.getId())
                .ifPresent(e -> {
                    throw new BadRequestException("You are already clocked in since " + e.getClockInAt());
                });

        LocalDate today = LocalDate.now();
        LocalDate weekStart = weekStartOf(today);
        weeklyTimesheetRepository.findByEmployeeIdAndWeekStartDate(employee.getId(), weekStart)
                .filter(ts -> ts.getStatus() == TimesheetStatus.SUBMITTED || ts.getStatus() == TimesheetStatus.APPROVED)
                .ifPresent(ts -> {
                    throw new BadRequestException("This week's timesheet has already been submitted for review");
                });

        TimeEntry entry = TimeEntry.builder()
                .employee(employee)
                .clockInAt(LocalDateTime.now())
                .clockInLat(request.getLatitude())
                .clockInLng(request.getLongitude())
                .notes(request.getNotes())
                .build();
        entry = timeEntryRepository.save(entry);

        return toResponse(entry);
    }

    @Transactional
    public TimeEntryResponse clockOut(Long userId, ClockOutRequest request) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        TimeEntry entry = timeEntryRepository.findFirstByEmployeeIdAndClockOutAtIsNullOrderByClockInAtDesc(employee.getId())
                .orElseThrow(() -> new BadRequestException("You are not currently clocked in"));

        entry.setClockOutAt(LocalDateTime.now());
        entry.setClockOutLat(request.getLatitude());
        entry.setClockOutLng(request.getLongitude());
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            entry.setNotes(entry.getNotes() == null ? request.getNotes() : entry.getNotes() + " | " + request.getNotes());
        }
        entry = timeEntryRepository.save(entry);

        recalculateWeeklyTotal(employee, weekStartOf(entry.getClockInAt().toLocalDate()));

        return toResponse(entry);
    }

    private void recalculateWeeklyTotal(Employee employee, LocalDate weekStart) {
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekStart.plusDays(7).atStartOfDay();

        List<TimeEntry> entries = timeEntryRepository.findByEmployeeIdAndClockInAtBetweenOrderByClockInAtAsc(
                employee.getId(), start, end);

        double totalHours = entries.stream().mapToDouble(TimeEntry::durationHours).sum();

        WeeklyTimesheet timesheet = weeklyTimesheetRepository.findByEmployeeIdAndWeekStartDate(employee.getId(), weekStart)
                .orElseGet(() -> WeeklyTimesheet.builder()
                        .employee(employee)
                        .weekStartDate(weekStart)
                        .status(TimesheetStatus.OPEN)
                        .build());

        timesheet.setTotalHours(Math.round(totalHours * 100.0) / 100.0);
        weeklyTimesheetRepository.save(timesheet);
    }

    public TimeEntryResponse getActiveEntry(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        return timeEntryRepository.findFirstByEmployeeIdAndClockOutAtIsNullOrderByClockInAtDesc(employee.getId())
                .map(this::toResponse)
                .orElse(null);
    }

    public List<TimeEntryResponse> getRecentEntries(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        return timeEntryRepository.findByEmployeeIdOrderByClockInAtDesc(employee.getId()).stream()
                .limit(50)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WeeklyTimesheetResponse submitCurrentWeek(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        timeEntryRepository.findFirstByEmployeeIdAndClockOutAtIsNullOrderByClockInAtDesc(employee.getId())
                .ifPresent(e -> {
                    throw new BadRequestException("Clock out of your current session before submitting this week's timesheet");
                });

        LocalDate weekStart = weekStartOf(LocalDate.now());
        recalculateWeeklyTotal(employee, weekStart);

        WeeklyTimesheet timesheet = weeklyTimesheetRepository.findByEmployeeIdAndWeekStartDate(employee.getId(), weekStart)
                .orElseThrow(() -> new BadRequestException("No time entries recorded for this week yet"));

        if (timesheet.getStatus() != TimesheetStatus.OPEN && timesheet.getStatus() != TimesheetStatus.REJECTED) {
            throw new BadRequestException("This week's timesheet has already been submitted");
        }

        timesheet.setStatus(TimesheetStatus.SUBMITTED);
        timesheet.setSubmittedAt(LocalDateTime.now());
        timesheet = weeklyTimesheetRepository.save(timesheet);

        return toResponse(timesheet, List.of());
    }

    public List<WeeklyTimesheetResponse> getMyTimesheets(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        return weeklyTimesheetRepository.findByEmployeeIdOrderByWeekStartDateDesc(employee.getId()).stream()
                .map(ts -> toResponse(ts, entriesFor(ts)))
                .collect(Collectors.toList());
    }

    public List<WeeklyTimesheetResponse> getPendingForReview(Long companyId, Long reviewerUserId, boolean isAdmin) {
        if (isAdmin) {
            return weeklyTimesheetRepository.findByEmployeeCompanyIdAndStatusOrderByWeekStartDateAsc(companyId, TimesheetStatus.SUBMITTED)
                    .stream()
                    .map(ts -> toResponse(ts, entriesFor(ts)))
                    .collect(Collectors.toList());
        }

        Employee manager = employeeRepository.findByUserId(reviewerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        return weeklyTimesheetRepository.findByEmployeeManagerIdAndStatus(manager.getId(), TimesheetStatus.SUBMITTED)
                .stream()
                .map(ts -> toResponse(ts, entriesFor(ts)))
                .collect(Collectors.toList());
    }

    @Transactional
    public WeeklyTimesheetResponse reviewTimesheet(Long reviewerUserId, Long timesheetId, TimesheetReviewRequest request) {
        if (request.getStatus() != TimesheetStatus.APPROVED && request.getStatus() != TimesheetStatus.REJECTED) {
            throw new BadRequestException("Review status must be either APPROVED or REJECTED");
        }

        Employee reviewer = employeeRepository.findByUserId(reviewerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer profile not found"));

        WeeklyTimesheet timesheet = weeklyTimesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found"));

        if (!timesheet.getEmployee().getCompany().getId().equals(reviewer.getCompany().getId())) {
            throw new ResourceNotFoundException("Timesheet not found");
        }

        boolean isAdmin = reviewer.getUser().getRole() == com.hrms.enums.UserRole.ROLE_ADMIN;
        boolean isDirectManager = timesheet.getEmployee().getManager() != null
                && timesheet.getEmployee().getManager().getId().equals(reviewer.getId());
        if (!isAdmin && !isDirectManager) {
            throw new BadRequestException("You can only review timesheets for your direct reports");
        }

        if (timesheet.getStatus() != TimesheetStatus.SUBMITTED) {
            throw new BadRequestException("This timesheet is not awaiting review");
        }

        timesheet.setStatus(request.getStatus());
        timesheet.setReviewedBy(reviewer);
        timesheet.setReviewComment(request.getReviewComment());
        timesheet.setReviewedAt(LocalDateTime.now());

        timesheet = weeklyTimesheetRepository.save(timesheet);
        return toResponse(timesheet, entriesFor(timesheet));
    }

    private List<TimeEntryResponse> entriesFor(WeeklyTimesheet ts) {
        LocalDateTime start = ts.getWeekStartDate().atStartOfDay();
        LocalDateTime end = ts.getWeekStartDate().plusDays(7).atStartOfDay();
        return timeEntryRepository
                .findByEmployeeIdAndClockInAtBetweenOrderByClockInAtAsc(ts.getEmployee().getId(), start, end).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private TimeEntryResponse toResponse(TimeEntry entry) {
        return TimeEntryResponse.builder()
                .id(entry.getId())
                .clockInAt(entry.getClockInAt())
                .clockInLat(entry.getClockInLat())
                .clockInLng(entry.getClockInLng())
                .clockOutAt(entry.getClockOutAt())
                .clockOutLat(entry.getClockOutLat())
                .clockOutLng(entry.getClockOutLng())
                .durationHours(Math.round(entry.durationHours() * 100.0) / 100.0)
                .notes(entry.getNotes())
                .isActive(entry.getClockOutAt() == null)
                .build();
    }

    private WeeklyTimesheetResponse toResponse(WeeklyTimesheet ts, List<TimeEntryResponse> entries) {
        Employee e = ts.getEmployee();
        return WeeklyTimesheetResponse.builder()
                .id(ts.getId())
                .employeeId(e.getId())
                .employeeName(e.getFirstName() + " " + e.getLastName())
                .weekStartDate(ts.getWeekStartDate())
                .weekEndDate(ts.getWeekStartDate().plusDays(6))
                .totalHours(ts.getTotalHours())
                .status(ts.getStatus())
                .submittedAt(ts.getSubmittedAt())
                .reviewedByName(ts.getReviewedBy() != null ? ts.getReviewedBy().getFirstName() + " " + ts.getReviewedBy().getLastName() : null)
                .reviewComment(ts.getReviewComment())
                .entries(entries)
                .build();
    }
}
