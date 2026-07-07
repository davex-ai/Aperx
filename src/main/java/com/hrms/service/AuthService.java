package com.hrms.service;

import com.hrms.dto.request.CompleteSignupRequest;
import com.hrms.dto.request.CreateEmployeeRequest;
import com.hrms.dto.request.FirstLoginProfileRequest;
import com.hrms.dto.request.LoginRequest;
import com.hrms.dto.response.AuthResponse;
import com.hrms.entity.*;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.*;
import com.hrms.security.JwtUtil;
import com.hrms.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    public record EmployeeCreationResult(Employee employee, boolean invitationSent) {}

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final BankAccountRepository bankAccountRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    private static final SecureRandom RANDOM = new SecureRandom();

    public AuthResponse login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
        var authentication = authenticationManager.authenticate(authToken);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), principal.getAuthorities().stream().toList());

        Employee employee = employeeRepository.findByUserId(user.getId()).orElse(null);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .mustCompleteOnboarding(Boolean.TRUE.equals(user.getMustChangePassword()))
                .employeeId(employee != null ? employee.getId() : null)
                .fullName(employee != null ? employee.getFirstName() + " " + employee.getLastName() : null)
                .build();
    }
    @Transactional
    public EmployeeCreationResult createEmployeeAccount(CreateEmployeeRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("An account with this email already exists");
        }

        String placeholderHash = passwordEncoder.encode(generateSecureToken());

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(placeholderHash)
                .role(request.getRole())
                .isActive(true)
                .mustChangePassword(true)
                .build();
        user = userRepository.save(user);

        Employee manager = null;
        if (request.getManagerId() != null) {
            manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id " + request.getManagerId()));
        }

        Employee employee = Employee.builder()
                .user(user)
                .manager(manager)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .department(request.getDepartment())
                .jobTitle(request.getJobTitle())
                .salaryGrade(request.getSalaryGrade())
                .hireDate(request.getHireDate())
                .salaryAmount(request.getSalaryAmount())
                .build();
        employee = employeeRepository.save(employee);

        String token = generateSecureToken();
        VerificationToken verificationToken = VerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(48))
                .used(false)
                .build();
        verificationTokenRepository.save(verificationToken);

        boolean invitationSent = true;
        try {
            emailService.sendVerificationEmail(user.getEmail(), employee.getFirstName(), token);
        } catch (ResendClient.EmailDeliveryException e) {
            invitationSent = false;
        }

        return new EmployeeCreationResult(employee, invitationSent);
    }

    @Transactional
    public boolean resendInvitation(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        User user = employee.getUser();

        if (!Boolean.TRUE.equals(user.getMustChangePassword())) {
            throw new BadRequestException("This employee has already completed onboarding");
        }

        String token = generateSecureToken();
        VerificationToken verificationToken = VerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(48))
                .used(false)
                .build();
        verificationTokenRepository.save(verificationToken);

        try {
            emailService.sendVerificationEmail(user.getEmail(), employee.getFirstName(), token);
            return true;
        } catch (ResendClient.EmailDeliveryException e) {
            return false;
        }
    }

    @Transactional
    public void completeSignup(CompleteSignupRequest request) {
        VerificationToken vt = verificationTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or unknown verification link"));

        if (Boolean.TRUE.equals(vt.getUsed())) {
            throw new BadRequestException("This verification link has already been used");
        }
        if (vt.isExpired()) {
            throw new BadRequestException("This verification link has expired. Please contact HR for a new one.");
        }

        User user = vt.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        vt.setUsed(true);
        verificationTokenRepository.save(vt);
    }

    @Transactional
    public void completeFirstLoginProfile(Long userId, FirstLoginProfileRequest request) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        employee.setPhoneNumber(request.getPhoneNumber());
        employeeRepository.save(employee);

        if (request.getBankName() != null && !request.getBankName().isBlank()) {
            BankAccount bankAccount = bankAccountRepository.findByEmployeeId(employee.getId())
                    .orElse(BankAccount.builder().employee(employee).build());
            bankAccount.setBankName(request.getBankName());
            bankAccount.setAccountHolderName(request.getAccountHolderName());
            bankAccount.setAccountNumber(request.getAccountNumber());
            bankAccount.setRoutingNumber(request.getRoutingNumber());
            bankAccountRepository.save(bankAccount);
        }

        if (request.getEmergencyContactName() != null && !request.getEmergencyContactName().isBlank()) {
            EmergencyContact contact = EmergencyContact.builder()
                    .employee(employee)
                    .contactName(request.getEmergencyContactName())
                    .relationship(request.getEmergencyRelationship())
                    .phoneNumber(request.getEmergencyPhoneNumber())
                    .build();
            emergencyContactRepository.save(contact);
        }

        User user = employee.getUser();
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
