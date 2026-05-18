package com.booker.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBranchRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank String address,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String country,
        @Size(max = 20) String postalCode,
        Double latitude,
        Double longitude,
        @Size(max = 30) String phone,
        @Size(max = 255) String email,
        @Size(max = 60) String timezone,
        Boolean isPrimary
) {}
