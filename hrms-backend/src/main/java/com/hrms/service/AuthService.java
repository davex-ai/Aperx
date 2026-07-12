package com.hrms.service;

import com.hrms.dto.request.AssignCompanyEmailRequest;
import com.hrms.dto.request.CompleteSignupRequest;
import com.hrms.dto.request.CreateEmployeeRequest;
import com.hrms.dto.request.FirstLoginProfileRequest;
import com.hrms.dto.request.LoginRequest;
import com.hrms.dto.request.RegisterCompanyRequest;
import com.hrms.dto.response.AuthResponse;
import com.hrms.entity.*;
import com.hrms.enums.UserRole;
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

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
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

        if (!Boolean.TRUE.equals(user.getCompany().getIsActive())) {
            throw new BadRequestException("This company's account has been deactivated. Contact support.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), principal.getAuthorities().stream().toList());

        Employee employee = employeeRepository.findByUserId(user.getId()).orElse(null);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .companySlug(user.getCompany().getSlug())
                .role(user.getRole().name())
                .mustCompleteOnboarding(Boolean.TRUE.equals(user.getMustChangePassword()))
                .employeeId(employee != null ? employee.getId() : null)
                .fullName(employee != null ? employee.getFirstName() + " " + employee.getLastName() : null)
                .companyId(user.getCompany().getId())
                .companyName(user.getCompany().getName())
                .build();
    }

    @Transactional
    public AuthResponse registerCompany(RegisterCompanyRequest request) {
        if (userRepository.existsByEmail(request.getAdminEmail())) {
            throw new BadRequestException("An account with this email already exists");
        }

        String slug = generateUniqueSlug(request.getCompanyName());

        Company company = Company.builder()
                .name(request.getCompanyName())
                .slug(slug)
                .billingEmail(request.getAdminEmail())
                .isActive(true)
                .build();
        company = companyRepository.save(company);

        User adminUser = User.builder()
                .company(company)
                .email(request.getAdminEmail())
                .personalEmail(request.getAdminEmail())
                .passwordHash(passwordEncoder.encode(request.getAdminPassword()))
                .role(UserRole.ROLE_ADMIN)
                .isActive(true)
                .mustChangePassword(false)
                .build();
        adminUser = userRepository.save(adminUser);

        Employee adminEmployee = Employee.builder()
                .company(company)
                .user(adminUser)
                .firstName(request.getAdminFirstName())
                .lastName(request.getAdminLastName())
                .department("Executive")
                .jobTitle("Administrator")
                .hireDate(LocalDate.now())
                .salaryAmount(BigDecimal.ZERO)
                .build();
        employeeRepository.save(adminEmployee);

        String token = jwtUtil.generateToken(
                adminUser.getId(),
                adminUser.getEmail(),
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));

        return AuthResponse.builder()
                .token(token)
                .email(adminUser.getEmail())
                .role(adminUser.getRole().name())
                .companySlug(company.getSlug())
                .mustCompleteOnboarding(false)
                .employeeId(adminEmployee.getId())
                .fullName(request.getAdminFirstName() + " " + request.getAdminLastName())
                .companyId(company.getId())
                .companyName(company.getName())
                .build();
    }

    private String generateUniqueSlug(String companyName) {
        String base = companyName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.isBlank()) base = "company";

        String candidate = base;
        int suffix = 1;
        while (companyRepository.existsBySlug(candidate)) {
            suffix++;
            candidate = base + "-" + suffix;
        }
        return candidate;
    }

    @Transactional
    public Employee createDraftEmployee(Long companyId, CreateEmployeeRequest request) {
        if (userRepository.existsByEmail(request.getPersonalEmail())) {
            throw new BadRequestException("An account with this personal email already exists");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        String placeholderHash = passwordEncoder.encode(generateSecureToken());

        User user = User.builder()
                .company(company)
                .email(null)
                .personalEmail(request.getPersonalEmail())
                .passwordHash(placeholderHash)
                .role(request.getRole())
                .isActive(true)
                .mustChangePassword(true)
                .build();
        user = userRepository.save(user);

        Employee manager = null;
        if (request.getManagerId() != null) {
            manager = employeeRepository.findByIdAndCompanyId(request.getManagerId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id " + request.getManagerId()));
        }

        Employee employee = Employee.builder()
                .company(company)
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
                .payType(request.getPayType())
                .hourlyRate(request.getHourlyRate())
                .build();
        return employeeRepository.save(employee);
    }

    @Transactional
    public boolean assignCompanyEmail(Long companyId, Long employeeId, AssignCompanyEmailRequest request) {
        Employee employee = employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        User user = employee.getUser();

        if (user.getEmail() != null) {
            throw new BadRequestException("A company email has already been assigned to this employee");
        }
        if (userRepository.existsByEmail(request.getCompanyEmail())) {
            throw new BadRequestException("This company email is already in use");
        }

        user.setEmail(request.getCompanyEmail());
        userRepository.save(user);

        return sendInvite(employee, user);
    }

    @Transactional
    public boolean resendInvitation(Long companyId, Long employeeId) {
        Employee employee = employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        User user = employee.getUser();

        if (user.getEmail() == null) {
            throw new BadRequestException("Assign a company email to this employee before sending an invitation");
        }
        if (!Boolean.TRUE.equals(user.getMustChangePassword())) {
            throw new BadRequestException("This employee has already completed onboarding");
        }

        return sendInvite(employee, user);
    }

    private boolean sendInvite(Employee employee, User user) {
        String token = generateSecureToken();
        VerificationToken verificationToken = VerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(48))
                .used(false)
                .build();
        verificationTokenRepository.save(verificationToken);

        try {
            emailService.sendVerificationEmail(user.getPersonalEmail(), employee.getFirstName(), employee.getCompany().getName(), user.getEmail(), token);

            return true;
        } catch (RuntimeException e) {
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
