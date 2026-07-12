package com.hrms.controller;

import com.hrms.dto.request.AnonymousReportRequest;
import com.hrms.dto.request.ReportCommentRequest;
import com.hrms.dto.request.ReportStatusUpdateRequest;
import com.hrms.dto.response.AnonymousReportResponse;
import com.hrms.dto.response.ReportCommentResponse;
import com.hrms.service.AnonymousReportService;
import com.hrms.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class AnonymousReportController {

    private final AnonymousReportService reportService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<List<AnonymousReportResponse>> getFeed() {
        return ResponseEntity.ok(reportService.getFeed(authUtil.getCurrentCompanyId(), authUtil.getCurrentUserId()));
    }

    @PostMapping
    public ResponseEntity<AnonymousReportResponse> create(@Valid @RequestBody AnonymousReportRequest request) {
        return ResponseEntity.ok(
                reportService.createReport(authUtil.getCurrentCompanyId(), authUtil.getCurrentUserId(), request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnonymousReportResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody ReportStatusUpdateRequest request) {
        return ResponseEntity.ok(reportService.updateStatus(authUtil.getCurrentCompanyId(), id, request));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<ReportCommentResponse>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(
                reportService.getComments(authUtil.getCurrentCompanyId(), authUtil.getCurrentUserId(), id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ReportCommentResponse> addComment(@PathVariable Long id, @Valid @RequestBody ReportCommentRequest request) {
        return ResponseEntity.ok(
                reportService.addComment(authUtil.getCurrentCompanyId(), authUtil.getCurrentUserId(), id, request));
    }
}
