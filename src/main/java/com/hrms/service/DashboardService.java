package com.hrms.service;

import com.hrms.dto.response.DashboardStatsResponse;
import com.hrms.entity.Employee;
import com.hrms.enums.JobStatus;
import com.hrms.enums.LeaveStatus;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.JobPostingRepository;
import com.hrms.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final JobPostingRepository jobPostingRepository;

    public DashboardStatsResponse getStats() {
        List<Employee> all = employeeRepository.findAll();

        long totalHeadcount = all.stream()
                .filter(e -> e.getUser() != null && Boolean.TRUE.equals(e.getUser().getIsActive()))
                .count();

        LocalDate now = LocalDate.now();
        long newHiresThisMonth = all.stream()
                .filter(e -> e.getHireDate() != null
                        && e.getHireDate().getMonth() == now.getMonth()
                        && e.getHireDate().getYear() == now.getYear())
                .count();

        long inactiveThisYear = all.stream()
                .filter(e -> e.getUser() != null && !Boolean.TRUE.equals(e.getUser().getIsActive()))
                .count();

        double turnoverRate = totalHeadcount > 0
                ? (inactiveThisYear * 100.0) / (totalHeadcount + inactiveThisYear)
                : 0.0;

        Map<String, Long> departmentDistribution = all.stream()
                .filter(e -> e.getDepartment() != null)
                .collect(Collectors.groupingBy(Employee::getDepartment, LinkedHashMap::new, Collectors.counting()));

        long pendingLeave = leaveRequestRepository.countByStatus(LeaveStatus.PENDING);
        long openJobs = jobPostingRepository.findByStatus(JobStatus.OPEN).size();

        List<DashboardStatsResponse.MonthlyHeadcount> trend = buildHeadcountTrend(all);

        return DashboardStatsResponse.builder()
                .totalHeadcount(totalHeadcount)
                .newHiresThisMonth(newHiresThisMonth)
                .exitsThisYear(inactiveThisYear)
                .turnoverRatePercent(Math.round(turnoverRate * 100.0) / 100.0)
                .pendingLeaveRequests(pendingLeave)
                .openJobPostings(openJobs)
                .departmentDistribution(departmentDistribution)
                .headcountTrend(trend)
                .build();
    }

    private List<DashboardStatsResponse.MonthlyHeadcount> buildHeadcountTrend(List<Employee> all) {
        YearMonth current = YearMonth.now();
        List<DashboardStatsResponse.MonthlyHeadcount> trend = new java.util.ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            long count = all.stream()
                    .filter(e -> e.getHireDate() != null && !e.getHireDate().isAfter(ym.atEndOfMonth()))
                    .count();
            trend.add(DashboardStatsResponse.MonthlyHeadcount.builder()
                    .month(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + ym.getYear())
                    .count(count)
                    .build());
        }
        return trend;
    }
}
