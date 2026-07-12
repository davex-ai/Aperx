package com.hrms.util;

import com.hrms.entity.User;
import com.hrms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUtil {

    private final UserRepository userRepository;

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
    }

    public Long getCurrentCompanyId() {
        return getCurrentUser().getCompany().getId();
    }

    public boolean isPrivileged() {
        return getCurrentUser().getRole().name().equals("ROLE_ADMIN")
                || getCurrentUser().getRole().name().equals("ROLE_MANAGER");
    }
}
