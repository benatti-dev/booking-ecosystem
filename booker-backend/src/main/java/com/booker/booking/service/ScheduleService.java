package com.booker.booking.service;

import com.booker.booking.dto.*;
import com.booker.booking.entity.ScheduleBreak;
import com.booker.booking.entity.ScheduleOverride;
import com.booker.booking.entity.ScheduleRule;
import com.booker.booking.repository.ScheduleBreakRepository;
import com.booker.booking.repository.ScheduleOverrideRepository;
import com.booker.booking.repository.ScheduleRuleRepository;
import com.booker.business.entity.BookableResource;
import com.booker.business.entity.Branch;
import com.booker.business.entity.Employee;
import com.booker.business.repository.BranchRepository;
import com.booker.business.repository.EmployeeRepository;
import com.booker.business.repository.ResourceRepository;
import com.booker.shared.exception.BookerException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRuleRepository ruleRepository;
    private final ScheduleBreakRepository breakRepository;
    private final ScheduleOverrideRepository overrideRepository;
    private final EmployeeRepository employeeRepository;
    private final ResourceRepository resourceRepository;
    private final BranchRepository branchRepository;

    // ── Employee schedule ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public WeeklyScheduleResponse getEmployeeSchedule(Long employeeId) {
        findEmployeeOrThrow(employeeId);
        List<ScheduleRule> rules = ruleRepository.findByEmployeeId(employeeId);
        List<ScheduleOverride> overrides = overrideRepository.findUpcomingByEmployeeId(employeeId, LocalDate.now());

        return new WeeklyScheduleResponse(employeeId, null, toRuleDtos(rules), toOverrideDtos(overrides));
    }

    @Transactional
    public WeeklyScheduleResponse saveEmployeeSchedule(Long employeeId, List<ScheduleRuleDto> ruleDtos) {
        Employee employee = findEmployeeOrThrow(employeeId);

        // Delete existing rules for this employee
        ruleRepository.deleteAll(ruleRepository.findByEmployeeId(employeeId));

        for (ScheduleRuleDto dto : ruleDtos) {
            Branch branch = employee.getBranch();
            if (branch == null) {
                throw BookerException.badRequest("Employee must be assigned to a branch before setting schedule");
            }
            ScheduleRule rule = ScheduleRule.builder()
                    .employee(employee)
                    .branch(branch)
                    .dayOfWeek((short) dto.dayOfWeek())
                    .startTime(dto.startTime())
                    .endTime(dto.endTime())
                    .isWorkingDay(dto.isWorkingDay())
                    .build();
            ScheduleRule savedRule = ruleRepository.save(rule);

            if (dto.breaks() != null) {
                for (ScheduleBreakDto breakDto : dto.breaks()) {
                    ScheduleBreak sb = ScheduleBreak.builder()
                            .scheduleRule(savedRule)
                            .startTime(breakDto.startTime())
                            .endTime(breakDto.endTime())
                            .build();
                    breakRepository.save(sb);
                }
            }
        }

        return getEmployeeSchedule(employeeId);
    }

    @Transactional
    public ScheduleOverrideDto addEmployeeOverride(Long employeeId, ScheduleOverrideDto dto) {
        Employee employee = findEmployeeOrThrow(employeeId);
        ScheduleOverride override = ScheduleOverride.builder()
                .employee(employee)
                .overrideDate(dto.overrideDate())
                .startTime(dto.startTime())
                .endTime(dto.endTime())
                .reason(dto.reason())
                .build();
        return toOverrideDto(overrideRepository.save(override));
    }

    // ── Resource schedule ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public WeeklyScheduleResponse getResourceSchedule(Long resourceId) {
        findResourceOrThrow(resourceId);
        List<ScheduleRule> rules = ruleRepository.findByResourceId(resourceId);
        return new WeeklyScheduleResponse(null, resourceId, toRuleDtos(rules), List.of());
    }

    @Transactional
    public WeeklyScheduleResponse saveResourceSchedule(Long resourceId, List<ScheduleRuleDto> ruleDtos) {
        BookableResource resource = findResourceOrThrow(resourceId);
        ruleRepository.deleteAll(ruleRepository.findByResourceId(resourceId));

        for (ScheduleRuleDto dto : ruleDtos) {
            ScheduleRule rule = ScheduleRule.builder()
                    .resource(resource)
                    .branch(resource.getBranch())
                    .dayOfWeek((short) dto.dayOfWeek())
                    .startTime(dto.startTime())
                    .endTime(dto.endTime())
                    .isWorkingDay(dto.isWorkingDay())
                    .build();
            ScheduleRule savedRule = ruleRepository.save(rule);

            if (dto.breaks() != null) {
                for (ScheduleBreakDto breakDto : dto.breaks()) {
                    breakRepository.save(ScheduleBreak.builder()
                            .scheduleRule(savedRule)
                            .startTime(breakDto.startTime())
                            .endTime(breakDto.endTime())
                            .build());
                }
            }
        }
        return getResourceSchedule(resourceId);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Employee findEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> BookerException.notFound("Employee not found: " + id));
    }

    private BookableResource findResourceOrThrow(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> BookerException.notFound("Resource not found: " + id));
    }

    private List<ScheduleRuleDto> toRuleDtos(List<ScheduleRule> rules) {
        if (rules.isEmpty()) return List.of();

        // Batch-fetch all breaks for all rules in a single query (prevents N+1)
        List<Long> ruleIds = rules.stream().map(ScheduleRule::getId).toList();
        Map<Long, List<ScheduleBreak>> breaksByRuleId = breakRepository
                .findByScheduleRuleIdIn(ruleIds)
                .stream()
                .collect(Collectors.groupingBy(b -> b.getScheduleRule().getId()));

        return rules.stream().map(r -> {
            List<ScheduleBreakDto> breakDtos = breaksByRuleId
                    .getOrDefault(r.getId(), List.of())
                    .stream()
                    .map(b -> new ScheduleBreakDto(b.getId(), b.getStartTime(), b.getEndTime()))
                    .toList();
            return new ScheduleRuleDto(r.getId(), r.getDayOfWeek(), r.getStartTime(), r.getEndTime(),
                    r.isWorkingDay(), breakDtos);
        }).toList();
    }

    private List<ScheduleOverrideDto> toOverrideDtos(List<ScheduleOverride> overrides) {
        return overrides.stream().map(this::toOverrideDto).toList();
    }

    private ScheduleOverrideDto toOverrideDto(ScheduleOverride o) {
        return new ScheduleOverrideDto(o.getId(), o.getOverrideDate(), o.getStartTime(), o.getEndTime(), o.getReason());
    }
}
