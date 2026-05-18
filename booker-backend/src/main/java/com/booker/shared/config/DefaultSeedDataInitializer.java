package com.booker.shared.config;

import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.auth.entity.UserStatus;
import com.booker.auth.repository.UserRepository;
import com.booker.business.entity.Business;
import com.booker.business.entity.BusinessCategory;
import com.booker.business.entity.BusinessStatus;
import com.booker.business.entity.ResourceType;
import com.booker.business.repository.BusinessCategoryRepository;
import com.booker.business.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds demo categories and a sample active business on first startup.
 * Runs after {@link DefaultAdminInitializer} (Order 2).
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DefaultSeedDataInitializer implements ApplicationRunner {

    private final BusinessCategoryRepository categoryRepository;
    private final BusinessRepository         businessRepository;
    private final UserRepository             userRepository;
    private final PasswordEncoder            passwordEncoder;

    // ── Categories ────────────────────────────────────────────────────────────

    private record CategoryDef(String name, String label, ResourceType resourceType) {}

    private static final List<CategoryDef> CATEGORIES = List.of(
            new CategoryDef("barbershop",      "Barbershop",        ResourceType.EMPLOYEE),
            new CategoryDef("beauty_salon",    "Beauty Salon",      ResourceType.EMPLOYEE),
            new CategoryDef("nail_studio",     "Nail Studio",       ResourceType.EMPLOYEE),
            new CategoryDef("massage",         "Massage Therapy",   ResourceType.EMPLOYEE),
            new CategoryDef("fitness",         "Fitness & Gym",     ResourceType.RESOURCE),
            new CategoryDef("medical_clinic",  "Medical Clinic",    ResourceType.EMPLOYEE),
            new CategoryDef("dental",          "Dental Clinic",     ResourceType.EMPLOYEE),
            new CategoryDef("photography",     "Photography",       ResourceType.EMPLOYEE),
            new CategoryDef("cleaning",        "Cleaning Service",  ResourceType.RESOURCE),
            new CategoryDef("auto_service",    "Auto Service",      ResourceType.RESOURCE)
    );

    // ── Demo business owner ───────────────────────────────────────────────────

    private static final String OWNER_EMAIL    = "owner@booker.app";
    private static final String OWNER_PASSWORD = "Owner1234!";
    private static final String OWNER_NAME     = "Demo Business Owner";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCategories();
        seedDemoBusiness();
    }

    // ── Seed helpers ──────────────────────────────────────────────────────────

    private void seedCategories() {
        for (CategoryDef def : CATEGORIES) {
            if (!categoryRepository.existsByName(def.name())) {
                categoryRepository.save(BusinessCategory.builder()
                        .name(def.name())
                        .label(def.label())
                        .resourceType(def.resourceType())
                        .build());
                log.info("Seeded category: {}", def.name());
            }
        }
    }

    private void seedDemoBusiness() {
        // Ensure a demo business owner exists
        User owner = userRepository.findByEmail(OWNER_EMAIL).orElseGet(() -> {
            User u = User.builder()
                    .email(OWNER_EMAIL)
                    .passwordHash(passwordEncoder.encode(OWNER_PASSWORD))
                    .fullName(OWNER_NAME)
                    .role(UserRole.BUSINESS_OWNER)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(u);
            log.info("Seeded demo business owner: {}", OWNER_EMAIL);
            return u;
        });

        // Seed one demo business per category (only the barbershop for now)
        categoryRepository.findByName("barbershop").ifPresent(category -> {
            boolean exists = businessRepository.findByOwnerId(owner.getId(),
                    org.springframework.data.domain.PageRequest.of(0, 1)).hasContent();
            if (!exists) {
                businessRepository.save(Business.builder()
                        .owner(owner)
                        .category(category)
                        .name("Demo Barbershop")
                        .description("A fully equipped barbershop in the city centre. " +
                                "Classic cuts, beard trims and hot-towel shaves.")
                        .status(BusinessStatus.ACTIVE)
                        .build());
                log.info("Seeded demo business: Demo Barbershop");
            }
        });
    }
}
