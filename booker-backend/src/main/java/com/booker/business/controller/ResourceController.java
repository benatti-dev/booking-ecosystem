package com.booker.business.controller;

import com.booker.business.dto.CreateResourceRequest;
import com.booker.business.dto.ResourceResponse;
import com.booker.business.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/businesses/{businessId}/resources")
@RequiredArgsConstructor
@Tag(name = "Resources", description = "Bookable resource management per business")
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN')")
    @Operation(summary = "Add bookable resource to business")
    public ResourceResponse create(
            @PathVariable Long businessId,
            @Valid @RequestBody CreateResourceRequest req,
            Authentication auth) {
        return resourceService.create(businessId, req, auth);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN')")
    @Operation(summary = "List resources of a business")
    public Page<ResourceResponse> list(
            @PathVariable Long businessId,
            Pageable pageable) {
        return resourceService.listByBusiness(businessId, pageable);
    }

    @GetMapping("/{resourceId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN')")
    @Operation(summary = "Get resource by id")
    public ResourceResponse getById(
            @PathVariable Long businessId,
            @PathVariable Long resourceId) {
        return resourceService.getById(businessId, resourceId);
    }

    @DeleteMapping("/{resourceId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN')")
    @Operation(summary = "Deactivate resource")
    public ResourceResponse deactivate(
            @PathVariable Long businessId,
            @PathVariable Long resourceId,
            Authentication auth) {
        return resourceService.deactivate(businessId, resourceId, auth);
    }
}
