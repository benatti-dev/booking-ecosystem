package com.booker.business.controller;

import com.booker.business.dto.CategoryResponse;
import com.booker.business.dto.CreateCategoryRequest;
import com.booker.business.service.BusinessCategoryService;
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
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Business category management")
public class BusinessCategoryController {

    private final BusinessCategoryService categoryService;

    @GetMapping
    @Operation(summary = "List all business categories (public)")
    public ResponseEntity<List<CategoryResponse>> listAll() {
        return ResponseEntity.ok(categoryService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID (public)")
    public ResponseEntity<CategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a business category (ADMIN only)")
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CreateCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.create(req));
    }
}
