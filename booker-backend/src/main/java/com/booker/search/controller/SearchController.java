package com.booker.search.controller;

import com.booker.search.dto.BusinessSearchResult;
import com.booker.search.service.SearchService;
import com.booker.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "Search")
public class SearchController {

    private final SearchService searchService;

    /**
     * Search businesses by text query, category, and/or geo proximity.
     *
     * @param lat        Latitude of the user (optional; requires lng when provided)
     * @param lng        Longitude of the user (optional; requires lat when provided)
     * @param radiusKm   Search radius in kilometres (default 5; ignored when no geo)
     * @param categoryId Filter by category ID (optional)
     * @param query      Text search against business name and description (optional)
     */
    @GetMapping("/businesses")
    @Operation(summary = "Search businesses by text / category / proximity")
    public PagedResponse<BusinessSearchResult> searchBusinesses(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "5") double radiusKm,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20) Pageable pageable) {

        return PagedResponse.of(searchService.searchBusinesses(lat, lng, radiusKm, categoryId, query, pageable));
    }
}
