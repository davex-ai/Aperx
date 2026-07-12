package com.hrms.controller;

import com.hrms.dto.request.CompleteSignupRequest;
import com.hrms.dto.request.FirstLoginProfileRequest;
import com.hrms.dto.request.LoginRequest;
import com.hrms.dto.request.RegisterCompanyRequest;
import com.hrms.dto.response.AuthResponse;
import com.hrms.service.AuthService;
import com.hrms.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthUtil authUtil;

    @PostMapping("/register-company")
    public ResponseEntity<AuthResponse> registerCompany(@Valid @RequestBody RegisterCompanyRequest request) {
        return ResponseEntity.ok(authService.registerCompany(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/complete-signup")
    public ResponseEntity<Void> completeSignup(@Valid @RequestBody CompleteSignupRequest request) {
        authService.completeSignup(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/first-login-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> completeFirstLoginProfile(@Valid @RequestBody FirstLoginProfileRequest request) {
        authService.completeFirstLoginProfile(authUtil.getCurrentUserId(), request);
        return ResponseEntity.ok().build();
    }
}
