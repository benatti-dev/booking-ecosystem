package com.booker.catalog.controller;

import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.business.service.BusinessService;
import com.booker.catalog.dto.CreateServiceRequest;
import com.booker.catalog.dto.ServiceResponse;
import com.booker.catalog.service.ServiceService;
import com.booker.shared.dto.PagedResponse;
import com.booker.shared.exception.BookerException;
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
@RequestMapping("/businesses/{businessId}/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Service catalog per business")
public class ServiceController {

    private final ServiceService serviceService;
    private final BusinessService businessService;

    @GetMapping
    @Operation(summary = "List services for a business (public)")
    public ResponseEntity<PagedResponse<ServiceResponse>> list(
            @PathVariable Long businessId,
            @RequestParam(defaultValue = "true") Boolean activeOnly,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(PagedResponse.of(serviceService.listByBusiness(businessId, activeOnly, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    @Operation(summary = "Create a service")
    public ResponseEntity<ServiceResponse> create(
            @PathVariable Long businessId,
            @Valid @RequestBody CreateServiceRequest req,
            Authentication auth) {

        checkOwnerOrAdmin(businessId, auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceService.create(businessId, req));
    }

    @PutMapping("/{serviceId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    @Operation(summary = "Update a service")
    public ResponseEntity<ServiceResponse> update(
            @PathVariable Long businessId,
            @PathVariable Long serviceId,
            @Valid @RequestBody CreateServiceRequest req,
            Authentication auth) {

        checkOwnerOrAdmin(businessId, auth);
        return ResponseEntity.ok(serviceService.update(businessId, serviceId, req));
    }

    @DeleteMapping("/{serviceId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    @Operation(summary = "Deactivate a service")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long businessId,
            @PathVariable Long serviceId,
            Authentication auth) {

        checkOwnerOrAdmin(businessId, auth);
        serviceService.deactivate(businessId, serviceId);
        return ResponseEntity.noContent().build();
    }

    private void checkOwnerOrAdmin(Long businessId, Authentication auth) {
        User actor = (User) auth.getPrincipal();
        if (actor.getRole() == UserRole.ADMIN) return;
        var business = businessService.getById(businessId);
        if (!business.ownerId().equals(actor.getId())) {
            throw BookerException.forbidden("You do not own this business");
        }
    }
}
