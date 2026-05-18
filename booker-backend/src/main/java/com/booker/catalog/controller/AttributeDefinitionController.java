package com.booker.catalog.controller;

import com.booker.catalog.dto.AttributeDefinitionResponse;
import com.booker.catalog.dto.CreateAttributeDefinitionRequest;
import com.booker.catalog.dto.ServiceResponse;
import com.booker.catalog.service.AttributeDefinitionService;
import com.booker.catalog.service.ServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Services", description = "Service catalog — individual service detail")
public class AttributeDefinitionController {

    private final AttributeDefinitionService definitionService;
    private final ServiceService serviceService;

    /** GET /services/{id} — public */
    @GetMapping("/services/{id}")
    @Operation(summary = "Get service detail (public)")
    public ResponseEntity<ServiceResponse> getService(@PathVariable Long id) {
        return ResponseEntity.ok(serviceService.getById(id));
    }

    /** GET /categories/{id}/attribute-definitions — public */
    @GetMapping("/categories/{categoryId}/attribute-definitions")
    @Operation(summary = "Get attribute schema for a category (public)")
    public ResponseEntity<List<AttributeDefinitionResponse>> listDefinitions(
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(definitionService.getDefinitionResponses(categoryId));
    }

    /** POST /categories/{id}/attribute-definitions — ADMIN only */
    @PostMapping("/categories/{categoryId}/attribute-definitions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add attribute definition to a category (ADMIN)")
    public ResponseEntity<AttributeDefinitionResponse> createDefinition(
            @PathVariable Long categoryId,
            @Valid @RequestBody CreateAttributeDefinitionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(definitionService.create(categoryId, req));
    }

    /** DELETE /categories/{id}/attribute-definitions/{defId} — ADMIN only */
    @DeleteMapping("/categories/{categoryId}/attribute-definitions/{definitionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove attribute definition (ADMIN)")
    public ResponseEntity<Void> deleteDefinition(
            @PathVariable Long categoryId,
            @PathVariable Long definitionId) {
        definitionService.delete(categoryId, definitionId);
        return ResponseEntity.noContent().build();
    }
}
