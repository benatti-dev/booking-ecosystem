package com.booker.booking.controller;

import com.booker.booking.dto.*;
import com.booker.booking.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Schedule", description = "Employee and resource schedule management")
public class ScheduleController {

    private final ScheduleService scheduleService;

    // ── Employee schedule ──────────────────────────────────────

    @GetMapping("/employees/{employeeId}/schedule")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','EMPLOYEE','ADMIN')")
    @Operation(summary = "Get weekly schedule for employee")
    public WeeklyScheduleResponse getEmployeeSchedule(@PathVariable Long employeeId) {
        return scheduleService.getEmployeeSchedule(employeeId);
    }

    @PutMapping("/employees/{employeeId}/schedule")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN')")
    @Operation(summary = "Set weekly schedule for employee")
    public WeeklyScheduleResponse saveEmployeeSchedule(
            @PathVariable Long employeeId,
            @RequestBody List<ScheduleRuleDto> rules) {
        return scheduleService.saveEmployeeSchedule(employeeId, rules);
    }

    @PostMapping("/employees/{employeeId}/schedule/overrides")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','EMPLOYEE','ADMIN')")
    @Operation(summary = "Add schedule override (holiday, vacation)")
    public ScheduleOverrideDto addEmployeeOverride(
            @PathVariable Long employeeId,
            @Valid @RequestBody ScheduleOverrideDto dto) {
        return scheduleService.addEmployeeOverride(employeeId, dto);
    }

    // ── Resource schedule ─────────────────────────────────────

    @GetMapping("/resources/{resourceId}/schedule")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN')")
    @Operation(summary = "Get weekly schedule for resource")
    public WeeklyScheduleResponse getResourceSchedule(@PathVariable Long resourceId) {
        return scheduleService.getResourceSchedule(resourceId);
    }

    @PutMapping("/resources/{resourceId}/schedule")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN')")
    @Operation(summary = "Set weekly schedule for resource")
    public WeeklyScheduleResponse saveResourceSchedule(
            @PathVariable Long resourceId,
            @RequestBody List<ScheduleRuleDto> rules) {
        return scheduleService.saveResourceSchedule(resourceId, rules);
    }
}
