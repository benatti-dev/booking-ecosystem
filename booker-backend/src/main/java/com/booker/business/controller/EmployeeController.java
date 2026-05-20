package com.booker.business.controller;

import com.booker.business.dto.CreateEmployeeRequest;
import com.booker.business.dto.EmployeeResponse;
import com.booker.business.service.EmployeeService;
import com.booker.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/businesses/{businessId}/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Employee management per business")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN')")
    @Operation(summary = "Add employee to business")
    public EmployeeResponse create(
            @PathVariable Long businessId,
            @Valid @RequestBody CreateEmployeeRequest req,
            Authentication auth) {
        return employeeService.create(businessId, req, auth);
    }

    @GetMapping
    @Operation(summary = "List employees of a business")
    public PagedResponse<EmployeeResponse> list(
            @PathVariable Long businessId,
            Pageable pageable) {
        return PagedResponse.of(employeeService.listByBusiness(businessId, pageable));
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','EMPLOYEE','ADMIN')")
    @Operation(summary = "Get employee by id")
    public EmployeeResponse getById(
            @PathVariable Long businessId,
            @PathVariable Long employeeId) {
        return employeeService.getById(businessId, employeeId);
    }

    @DeleteMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN')")
    @Operation(summary = "Deactivate employee")
    public EmployeeResponse deactivate(
            @PathVariable Long businessId,
            @PathVariable Long employeeId,
            Authentication auth) {
        return employeeService.deactivate(businessId, employeeId, auth);
    }
}
