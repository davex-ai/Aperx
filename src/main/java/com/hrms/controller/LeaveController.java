package com.hrms.controller;

import com.hrms.dto.request.LeaveRequestCreate;
import com.hrms.dto.request.LeaveReviewRequest;
import com.hrms.dto.response.LeaveBalanceResponse;
import com.hrms.dto.response.LeaveRequestResponse;
import com.hrms.service.LeaveService;
import com.hrms.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final AuthUtil authUtil;

    @PostMapping
    public ResponseEntity<LeaveRequestResponse> submit(@Valid @RequestBody LeaveRequestCreate request) {
        return ResponseEntity.ok(leaveService.submitRequest(authUtil.getCurrentUserId(), request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<LeaveRequestResponse>> myRequests() {
        return ResponseEntity.ok(leaveService.getMyRequests(authUtil.getCurrentUserId()));
    }

    @GetMapping("/balances/me")
    public ResponseEntity<List<LeaveBalanceResponse>> myBalances() {
        return ResponseEntity.ok(leaveService.getMyBalances(authUtil.getCurrentUserId()));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<LeaveRequestResponse>> pendingRequests() {
        return ResponseEntity.ok(leaveService.getAllPending());
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<LeaveRequestResponse> review(@PathVariable Long id, @Valid @RequestBody LeaveReviewRequest request) {
        return ResponseEntity.ok(leaveService.reviewRequest(authUtil.getCurrentUserId(), id, request));
    }
}
