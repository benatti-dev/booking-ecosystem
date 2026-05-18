package com.booker.shared.config;

import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.auth.entity.UserStatus;
import com.booker.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultAdminInitializer implements ApplicationRunner {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@booker.app}")
    private String adminEmail;

    @Value("${app.admin.password:12345678}")
    private String adminPassword;

    @Value("${app.admin.full-name:Platform Admin}")
    private String adminFullName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(adminEmail)) {
            log.debug("Default admin already exists — skipping creation");
            return;
        }

        User admin = User.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .fullName(adminFullName)
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(admin);
        log.warn("Default admin created — email: {} — CHANGE THE PASSWORD IN PRODUCTION", adminEmail);
    }
}
