package com.hrms.config;

import com.hrms.entity.Employee;
import com.hrms.entity.User;
import com.hrms.enums.UserRole;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_ADMIN_EMAIL = "admin@company.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "ChangeMe123!";

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(DEFAULT_ADMIN_EMAIL)) {
            return;
        }

        User admin = User.builder()
                .email(DEFAULT_ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                .role(UserRole.ROLE_ADMIN)
                .isActive(true)
                .mustChangePassword(false)
                .build();
        admin = userRepository.save(admin);

        Employee adminProfile = Employee.builder()
                .user(admin)
                .firstName("System")
                .lastName("Administrator")
                .department("Human Resources")
                .jobTitle("HR Administrator")
                .hireDate(LocalDate.now())
                .salaryAmount(BigDecimal.ZERO)
                .build();
        employeeRepository.save(adminProfile);

        log.info("Seeded default admin account: {} / {}", DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD);
        log.warn("CHANGE THE DEFAULT ADMIN PASSWORD IMMEDIATELY IN PRODUCTION");
    }
}
