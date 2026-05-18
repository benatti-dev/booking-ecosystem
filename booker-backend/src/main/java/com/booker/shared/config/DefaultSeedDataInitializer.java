package com.booker.shared.config;

import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.auth.entity.UserStatus;
import com.booker.auth.repository.UserRepository;
import com.booker.booking.entity.ScheduleBreak;
import com.booker.booking.entity.ScheduleRule;
import com.booker.booking.repository.ScheduleBreakRepository;
import com.booker.booking.repository.ScheduleRuleRepository;
import com.booker.business.entity.*;
import com.booker.business.repository.*;
import com.booker.catalog.entity.AttributeFieldType;
import com.booker.catalog.entity.Service;
import com.booker.catalog.entity.ServiceAttributeDefinition;
import com.booker.catalog.repository.ServiceAttributeDefinitionRepository;
import com.booker.catalog.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Seeds all demo/default data on first startup (idempotent — safe to re-run).
 * Runs after {@link DefaultAdminInitializer} (Order 2).
 *
 * Seed accounts:
 *   owner@booker.app  / Owner1234!  — BUSINESS_OWNER
 *   client@booker.app / Client1234! — CLIENT
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DefaultSeedDataInitializer implements ApplicationRunner {

    private final BusinessCategoryRepository         categoryRepository;
    private final BusinessRepository                 businessRepository;
    private final BranchRepository                   branchRepository;
    private final EmployeeRepository                 employeeRepository;
    private final ServiceRepository                  serviceRepository;
    private final ServiceAttributeDefinitionRepository attributeDefinitionRepository;
    private final ScheduleRuleRepository             scheduleRuleRepository;
    private final ScheduleBreakRepository            scheduleBreakRepository;
    private final UserRepository                     userRepository;
    private final PasswordEncoder                    passwordEncoder;
    private final JdbcTemplate                       jdbcTemplate;

    // ── Categories ────────────────────────────────────────────────────────────

    private record CategoryDef(String name, String label, ResourceType resourceType) {}

    private static final List<CategoryDef> CATEGORIES = List.of(
            new CategoryDef("barbershop",     "Barbershop",        ResourceType.EMPLOYEE),
            new CategoryDef("beauty_salon",   "Beauty Salon",      ResourceType.EMPLOYEE),
            new CategoryDef("nail_studio",    "Nail Studio",       ResourceType.EMPLOYEE),
            new CategoryDef("spa_wellness",   "Spa & Wellness",    ResourceType.EMPLOYEE),
            new CategoryDef("massage",        "Massage Therapy",   ResourceType.EMPLOYEE),
            new CategoryDef("fitness_gym",    "Fitness & Gym",     ResourceType.EMPLOYEE),
            new CategoryDef("medical_clinic", "Medical Clinic",    ResourceType.EMPLOYEE),
            new CategoryDef("dental_clinic",  "Dental Clinic",     ResourceType.EMPLOYEE),
            new CategoryDef("photography",    "Photography",       ResourceType.EMPLOYEE),
            new CategoryDef("car_service",    "Car Service",       ResourceType.RESOURCE),
            new CategoryDef("coworking",      "Coworking Space",   ResourceType.RESOURCE),
            new CategoryDef("sports_court",   "Sports Court",      ResourceType.RESOURCE)
    );

    // ── Attribute definitions (by category name) ──────────────────────────────

    private record AttrDef(
            String fieldKey, String fieldLabel,
            AttributeFieldType fieldType, List<String> options,
            boolean required, int sortOrder) {}

    private static final Map<String, List<AttrDef>> ATTRIBUTE_DEFS = Map.ofEntries(
            Map.entry("beauty_salon", List.of(
                    new AttrDef("hair_type",   "Hair Type",   AttributeFieldType.SELECT,
                            List.of("Straight", "Wavy", "Curly", "Coily"), false, 1),
                    new AttrDef("hair_length", "Hair Length", AttributeFieldType.SELECT,
                            List.of("Short", "Medium", "Long", "Extra Long"), false, 2)
            )),
            Map.entry("barbershop", List.of(
                    new AttrDef("beard",      "Beard Trim", AttributeFieldType.BOOLEAN, null, false, 1),
                    new AttrDef("hair_style", "Hair Style", AttributeFieldType.SELECT,
                            List.of("Classic", "Fade", "Undercut", "Textured"), false, 2)
            )),
            Map.entry("nail_studio", List.of(
                    new AttrDef("nail_shape",  "Nail Shape",  AttributeFieldType.SELECT,
                            List.of("Square", "Round", "Oval", "Almond", "Stiletto"), false, 1),
                    new AttrDef("nail_length", "Nail Length", AttributeFieldType.SELECT,
                            List.of("Short", "Medium", "Long"), false, 2),
                    new AttrDef("gel_coat",    "Gel Coat",    AttributeFieldType.BOOLEAN, null, false, 3)
            )),
            Map.entry("spa_wellness", List.of(
                    new AttrDef("pressure", "Massage Pressure", AttributeFieldType.SELECT,
                            List.of("Light", "Medium", "Deep"), false, 1),
                    new AttrDef("area",     "Focus Area",       AttributeFieldType.SELECT,
                            List.of("Full Body", "Back", "Legs", "Face"), false, 2)
            )),
            Map.entry("fitness_gym", List.of(
                    new AttrDef("level",       "Fitness Level", AttributeFieldType.SELECT,
                            List.of("Beginner", "Intermediate", "Advanced"), false, 1),
                    new AttrDef("group_class", "Group Class",   AttributeFieldType.BOOLEAN, null, false, 2)
            )),
            Map.entry("medical_clinic", List.of(
                    new AttrDef("visit_type", "Visit Type", AttributeFieldType.SELECT,
                            List.of("First Visit", "Follow-up", "Consultation"), true, 1),
                    new AttrDef("symptoms",   "Symptoms",   AttributeFieldType.TEXT, null, false, 2)
            )),
            Map.entry("dental_clinic", List.of(
                    new AttrDef("procedure_type", "Procedure Type", AttributeFieldType.SELECT,
                            List.of("Cleaning", "Filling", "Extraction", "Whitening", "Root Canal"), true, 1),
                    new AttrDef("has_xray",       "X-Ray Required", AttributeFieldType.BOOLEAN, null, false, 2)
            )),
            Map.entry("car_service", List.of(
                    new AttrDef("car_make",      "Car Make",      AttributeFieldType.TEXT,   null, true, 1),
                    new AttrDef("car_model",     "Car Model",     AttributeFieldType.TEXT,   null, true, 2),
                    new AttrDef("license_plate", "License Plate", AttributeFieldType.TEXT,   null, true, 3),
                    new AttrDef("service_type",  "Service Type",  AttributeFieldType.SELECT,
                            List.of("Oil Change", "Tire Rotation", "Brake Service", "Full Service"), true, 4)
            )),
            Map.entry("coworking", List.of(
                    new AttrDef("workspace_type", "Workspace Type",   AttributeFieldType.SELECT,
                            List.of("Hot Desk", "Private Office", "Meeting Room"), true, 1),
                    new AttrDef("people_count",   "Number of People", AttributeFieldType.NUMBER, null, false, 2),
                    new AttrDef("need_monitor",   "Monitor Needed",   AttributeFieldType.BOOLEAN, null, false, 3)
            )),
            Map.entry("sports_court", List.of(
                    new AttrDef("sport",            "Sport",            AttributeFieldType.SELECT,
                            List.of("Tennis", "Basketball", "Football", "Volleyball", "Badminton"), true, 1),
                    new AttrDef("equipment_needed", "Equipment Needed", AttributeFieldType.BOOLEAN, null, false, 2)
            ))
    );

    // ── Demo credentials ─────────────────────────────────────────────────────

    private static final String OWNER_EMAIL    = "owner@booker.app";
    private static final String OWNER_PASSWORD = "Owner1234!";
    private static final String OWNER_NAME     = "Demo Business Owner";

    private static final String CLIENT_EMAIL    = "client@booker.app";
    private static final String CLIENT_PASSWORD = "Client1234!";
    private static final String CLIENT_NAME     = "Demo Client";

    // ── Entry point ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCategories();
        seedAttributeDefinitions();
        seedDemoUsers();
        seedDemoBusiness();
    }

    // ── Categories ────────────────────────────────────────────────────────────

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

    // ── Attribute definitions ─────────────────────────────────────────────────

    private void seedAttributeDefinitions() {
        for (Map.Entry<String, List<AttrDef>> entry : ATTRIBUTE_DEFS.entrySet()) {
            categoryRepository.findByName(entry.getKey()).ifPresent(category -> {
                List<ServiceAttributeDefinition> existing =
                        attributeDefinitionRepository.findByCategoryIdOrderBySortOrder(category.getId());
                for (AttrDef def : entry.getValue()) {
                    boolean alreadyExists = existing.stream()
                            .anyMatch(e -> e.getFieldKey().equals(def.fieldKey()));
                    if (!alreadyExists) {
                        attributeDefinitionRepository.save(ServiceAttributeDefinition.builder()
                                .category(category)
                                .fieldKey(def.fieldKey())
                                .fieldLabel(def.fieldLabel())
                                .fieldType(def.fieldType())
                                .options(def.options())
                                .isRequired(def.required())
                                .sortOrder(def.sortOrder())
                                .build());
                        log.info("Seeded attribute [{}.{}]", entry.getKey(), def.fieldKey());
                    }
                }
            });
        }
    }

    // ── Demo users ────────────────────────────────────────────────────────────

    private void seedDemoUsers() {
        if (userRepository.findByEmail(OWNER_EMAIL).isEmpty()) {
            userRepository.save(User.builder()
                    .email(OWNER_EMAIL)
                    .passwordHash(passwordEncoder.encode(OWNER_PASSWORD))
                    .fullName(OWNER_NAME)
                    .role(UserRole.BUSINESS_OWNER)
                    .status(UserStatus.ACTIVE)
                    .build());
            log.info("Seeded demo owner: {}", OWNER_EMAIL);
        }
        if (userRepository.findByEmail(CLIENT_EMAIL).isEmpty()) {
            userRepository.save(User.builder()
                    .email(CLIENT_EMAIL)
                    .passwordHash(passwordEncoder.encode(CLIENT_PASSWORD))
                    .fullName(CLIENT_NAME)
                    .role(UserRole.CLIENT)
                    .status(UserStatus.ACTIVE)
                    .build());
            log.info("Seeded demo client: {}", CLIENT_EMAIL);
        }
    }

    // ── Demo business ─────────────────────────────────────────────────────────

    private void seedDemoBusiness() {
        User owner = userRepository.findByEmail(OWNER_EMAIL).orElseThrow();

        // Skip if this owner already has a business
        if (businessRepository.findByOwnerId(owner.getId(),
                org.springframework.data.domain.PageRequest.of(0, 1)).hasContent()) {
            return;
        }

        BusinessCategory category = categoryRepository.findByName("beauty_salon").orElse(null);
        if (category == null) return;

        // Business
        Business business = businessRepository.save(Business.builder()
                .owner(owner)
                .category(category)
                .name("Glamour Beauty Studio")
                .description("A premium beauty salon offering haircuts, colouring, and styling.")
                .status(BusinessStatus.ACTIVE)
                .build());

        // Branch
        Branch branch = branchRepository.save(Branch.builder()
                .business(business)
                .name("Main Branch")
                .address("15 Khreshchatyk St")
                .city("Kyiv")
                .country("UA")
                .timezone("Europe/Kiev")
                .isPrimary(true)
                .build());

        // Employee
        Employee employee = employeeRepository.save(Employee.builder()
                .business(business)
                .branch(branch)
                .displayName("Anna Kovalenko")
                .position("Senior Stylist")
                .isActive(true)
                .build());

        // Services
        Service haircut = serviceRepository.save(Service.builder()
                .business(business).category(category)
                .name("Haircut & Styling")
                .description("Professional cut and blow-dry finish.")
                .durationMin(45).price(BigDecimal.valueOf(350)).build());

        Service colouring = serviceRepository.save(Service.builder()
                .business(business).category(category)
                .name("Full Hair Colouring")
                .description("Single-process colour with toner and conditioning treatment.")
                .durationMin(120).price(BigDecimal.valueOf(950)).build());

        Service blowDry = serviceRepository.save(Service.builder()
                .business(business).category(category)
                .name("Blow-dry")
                .description("Wash and blow-dry — perfect finish every time.")
                .durationMin(30).price(BigDecimal.valueOf(200)).build());

        // Link services to employee via join table
        for (Service svc : List.of(haircut, colouring, blowDry)) {
            jdbcTemplate.update(
                    "INSERT INTO service_employees (service_id, employee_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                    svc.getId(), employee.getId());
        }

        // Weekly schedule: Mon–Fri 09:00–19:00 (with lunch break), Sat 10:00–16:00, Sun off
        int[][] weekDays = {
                // {dayOfWeek, startH, startM, endH, endM, isWorking}
                {0,  9, 0, 19, 0, 0},  // Sun off
                {1,  9, 0, 19, 0, 1},  // Mon
                {2,  9, 0, 19, 0, 1},  // Tue
                {3,  9, 0, 19, 0, 1},  // Wed
                {4,  9, 0, 19, 0, 1},  // Thu
                {5,  9, 0, 19, 0, 1},  // Fri
                {6, 10, 0, 16, 0, 1},  // Sat
        };
        for (int[] d : weekDays) {
            ScheduleRule rule = scheduleRuleRepository.save(ScheduleRule.builder()
                    .employee(employee)
                    .branch(branch)
                    .dayOfWeek((short) d[0])
                    .startTime(LocalTime.of(d[1], d[2]))
                    .endTime(LocalTime.of(d[3], d[4]))
                    .isWorkingDay(d[5] == 1)
                    .build());
            // Lunch break 13:00-14:00 for Mon-Fri
            if (d[5] == 1 && d[0] >= 1 && d[0] <= 5) {
                scheduleBreakRepository.save(ScheduleBreak.builder()
                        .scheduleRule(rule)
                        .startTime(LocalTime.of(13, 0))
                        .endTime(LocalTime.of(14, 0))
                        .build());
            }
        }

        log.info("Seeded demo business: {} (branch: {}, employee: {})",
                business.getName(), branch.getName(), employee.getDisplayName());
    }
}
