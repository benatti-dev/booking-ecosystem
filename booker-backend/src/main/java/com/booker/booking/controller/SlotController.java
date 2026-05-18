package com.booker.booking.controller;

import com.booker.booking.dto.SlotResponse;
import com.booker.booking.service.SlotGeneratorService;
import com.booker.business.entity.BookableResource;
import com.booker.business.entity.Branch;
import com.booker.business.entity.Employee;
import com.booker.business.repository.EmployeeRepository;
import com.booker.business.repository.ResourceRepository;
import com.booker.shared.exception.BookerException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/slots")
@RequiredArgsConstructor
@Tag(name = "Slots", description = "Available time slot generation")
public class SlotController {

    private final SlotGeneratorService slotGeneratorService;
    private final EmployeeRepository employeeRepository;
    private final ResourceRepository resourceRepository;

    @GetMapping
    @Operation(summary = "Get available booking slots for a service on a given date")
    public SlotResponse getAvailableSlots(
            @RequestParam Long serviceId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        if (employeeId == null && resourceId == null) {
            throw BookerException.badRequest("Either employeeId or resourceId must be provided");
        }

        List<LocalTime> slots;
        Long resolvedEmployeeId = null;
        Long resolvedResourceId = null;

        if (employeeId != null) {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> BookerException.notFound("Employee not found: " + employeeId));
            String timezone = resolveTimezone(employee.getBranch());
            slots = slotGeneratorService.getAvailableSlotsForEmployee(employeeId, serviceId, date, timezone);
            resolvedEmployeeId = employeeId;
        } else {
            BookableResource resource = resourceRepository.findById(resourceId)
                    .orElseThrow(() -> BookerException.notFound("Resource not found: " + resourceId));
            String timezone = resolveTimezone(resource.getBranch());
            slots = slotGeneratorService.getAvailableSlotsForResource(resourceId, serviceId, date, timezone);
            resolvedResourceId = resourceId;
        }

        return new SlotResponse(date.toString(), resolvedEmployeeId, resolvedResourceId,
                serviceId, 0, slots);
    }

    private String resolveTimezone(Branch branch) {
        if (branch == null) return "UTC";
        String tz = branch.getTimezone();
        return (tz != null && !tz.isBlank()) ? tz : "UTC";
    }
}
