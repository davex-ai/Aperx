package com.hrms.service;

import com.hrms.dto.request.AnonymousReportRequest;
import com.hrms.dto.request.ReportCommentRequest;
import com.hrms.dto.request.ReportStatusUpdateRequest;
import com.hrms.dto.response.AnonymousReportResponse;
import com.hrms.dto.response.ReportCommentResponse;
import com.hrms.entity.AnonymousReport;
import com.hrms.entity.AnonymousReportComment;
import com.hrms.entity.Company;
import com.hrms.entity.Employee;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.AnonymousReportCommentRepository;
import com.hrms.repository.AnonymousReportRepository;
import com.hrms.repository.CompanyRepository;
import com.hrms.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnonymousReportService {

    private final AnonymousReportRepository reportRepository;
    private final AnonymousReportCommentRepository commentRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final AnonymousHandleGenerator handleGenerator;

    @Transactional
    public AnonymousReportResponse createReport(Long companyId, Long userId, AnonymousReportRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        Employee submitter = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        AnonymousReport report = AnonymousReport.builder()
                .company(company)
                .submittedBy(submitter)
                .displayHandle(handleGenerator.generate())
                .title(request.getTitle())
                .body(request.getBody())
                .category(request.getCategory())
                .build();
        report = reportRepository.save(report);

        return toResponse(report, submitter);
    }

    public List<AnonymousReportResponse> getFeed(Long companyId, Long userId) {
        Employee requester = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        return reportRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(r -> toResponse(r, requester))
                .collect(Collectors.toList());
    }

    @Transactional
    public AnonymousReportResponse updateStatus(Long companyId, Long reportId, ReportStatusUpdateRequest request) {
        AnonymousReport report = reportRepository.findByIdAndCompanyId(reportId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        report.setStatus(request.getStatus());
        report = reportRepository.save(report);
        return toResponse(report, report.getSubmittedBy());
    }

    @Transactional
    public ReportCommentResponse addComment(Long companyId, Long userId, Long reportId, ReportCommentRequest request) {
        Employee commenter = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        AnonymousReport report = reportRepository.findByIdAndCompanyId(reportId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        AnonymousReportComment comment = AnonymousReportComment.builder()
                .report(report)
                .commenter(commenter)
                .body(request.getBody())
                .build();
        comment = commentRepository.save(comment);

        return toResponse(comment, report, commenter);
    }

    public List<ReportCommentResponse> getComments(Long companyId, Long userId, Long reportId) {
        Employee requester = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        AnonymousReport report = reportRepository.findByIdAndCompanyId(reportId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        return commentRepository.findByReportIdOrderByCreatedAtAsc(reportId).stream()
                .map(c -> toResponse(c, report, requester))
                .collect(Collectors.toList());
    }

    private AnonymousReportResponse toResponse(AnonymousReport report, Employee requester) {
        return AnonymousReportResponse.builder()
                .id(report.getId())
                .displayHandle(report.getDisplayHandle())
                .title(report.getTitle())
                .body(report.getBody())
                .category(report.getCategory())
                .status(report.getStatus())
                .commentCount(commentRepository.countByReportId(report.getId()))
                .createdAt(report.getCreatedAt())
                .isMine(report.getSubmittedBy().getId().equals(requester.getId()))
                .build();
    }

    private ReportCommentResponse toResponse(AnonymousReportComment comment, AnonymousReport report, Employee requester) {
        boolean commenterIsReporter = comment.getCommenter().getId().equals(report.getSubmittedBy().getId());
        String displayName = commenterIsReporter
                ? report.getDisplayHandle()
                : comment.getCommenter().getFirstName() + " " + comment.getCommenter().getLastName();

        return ReportCommentResponse.builder()
                .id(comment.getId())
                .displayName(displayName)
                .isReporter(commenterIsReporter)
                .isMine(comment.getCommenter().getId().equals(requester.getId()))
                .body(comment.getBody())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
