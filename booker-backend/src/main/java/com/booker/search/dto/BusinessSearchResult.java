package com.booker.search.dto;

public record BusinessSearchResult(
        Long id,
        String name,
        String description,
        String logoUrl,
        Long categoryId,
        String categoryName,
        String categoryLabel,
        String city,
        String address,
        String timezone,
        Double distanceMeters
) {}
