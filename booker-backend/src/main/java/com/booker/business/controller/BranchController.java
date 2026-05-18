package com.booker.business.controller;

import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.business.dto.BranchResponse;
import com.booker.business.dto.CreateBranchRequest;
import com.booker.business.service.BranchService;
import com.booker.business.service.BusinessService;
import com.booker.shared.exception.BookerException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/businesses/{businessId}/branches")
@RequiredArgsConstructor
@Tag(name = "Branches", description = "Branch management per business")
public class BranchController {

    private final BranchService branchService;
    private final BusinessService businessService;

    @GetMapping
    @Operation(summary = "List all branches of a business (public)")
    public ResponseEntity<List<BranchResponse>> list(@PathVariable Long businessId) {
        return ResponseEntity.ok(branchService.listByBusiness(businessId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    @Operation(summary = "Add a branch to a business")
    public ResponseEntity<BranchResponse> create(
            @PathVariable Long businessId,
            @Valid @RequestBody CreateBranchRequest req,
            Authentication auth) {

        checkOwnerOrAdmin(businessId, auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(branchService.create(businessId, req));
    }

    @PutMapping("/{branchId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    @Operation(summary = "Update a branch")
    public ResponseEntity<BranchResponse> update(
            @PathVariable Long businessId,
            @PathVariable Long branchId,
            @Valid @RequestBody CreateBranchRequest req,
            Authentication auth) {

        checkOwnerOrAdmin(businessId, auth);
        return ResponseEntity.ok(branchService.update(businessId, branchId, req));
    }

    @DeleteMapping("/{branchId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    @Operation(summary = "Delete a branch")
    public ResponseEntity<Void> delete(
            @PathVariable Long businessId,
            @PathVariable Long branchId,
            Authentication auth) {

        checkOwnerOrAdmin(businessId, auth);
        branchService.delete(businessId, branchId);
        return ResponseEntity.noContent().build();
    }

    private void checkOwnerOrAdmin(Long businessId, Authentication auth) {
        User actor = (User) auth.getPrincipal();
        if (actor.getRole() == UserRole.ADMIN) return;
        // Verify the authenticated BUSINESS_OWNER owns this business
        var business = businessService.getById(businessId);
        if (!business.ownerId().equals(actor.getId())) {
            throw BookerException.forbidden("You do not own this business");
        }
    }
}
