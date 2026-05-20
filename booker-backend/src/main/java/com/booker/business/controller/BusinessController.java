package com.booker.business.controller;

import com.booker.auth.entity.User;
import com.booker.business.dto.*;
import com.booker.business.entity.BusinessStatus;
import com.booker.business.service.BusinessService;
import com.booker.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/businesses")
@RequiredArgsConstructor
@Tag(name = "Businesses", description = "Business registration and management")
public class BusinessController {

    private final BusinessService businessService;

    @PostMapping
    @PreAuthorize("hasRole('BUSINESS_OWNER')")
    @Operation(summary = "Register a new business")
    public ResponseEntity<BusinessResponse> create(
            @Valid @RequestBody CreateBusinessRequest req,
            Authentication auth) {

        User owner = (User) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(businessService.create(req, owner));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get business details (public)")
    public ResponseEntity<BusinessResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(businessService.getById(id));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    @Operation(summary = "List my businesses")
    public ResponseEntity<PagedResponse<BusinessResponse>> myBusinesses(
            Authentication auth,
            @PageableDefault(size = 20) Pageable pageable) {

        User owner = (User) auth.getPrincipal();
        return ResponseEntity.ok(PagedResponse.of(businessService.getMyBusinesses(owner.getId(), pageable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    @Operation(summary = "Update business info")
    public ResponseEntity<BusinessResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBusinessRequest req,
            Authentication auth) {

        return ResponseEntity.ok(businessService.update(id, req, auth));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change business status (ADMIN only)")
    public ResponseEntity<BusinessResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody BusinessStatusRequest req,
            Authentication auth) {

        return ResponseEntity.ok(businessService.changeStatus(id, req, auth));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List businesses by status (ADMIN)")
    public ResponseEntity<PagedResponse<BusinessResponse>> listByStatus(
            @RequestParam(defaultValue = "PENDING") BusinessStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(PagedResponse.of(businessService.getByStatus(status, pageable)));
    }
}
