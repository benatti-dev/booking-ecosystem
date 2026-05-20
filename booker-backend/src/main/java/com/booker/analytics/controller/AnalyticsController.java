package com.booker.analytics.controller;

import com.booker.analytics.dto.BusinessAnalyticsResponse;
import com.booker.analytics.dto.PlatformOverviewResponse;
import com.booker.analytics.service.AnalyticsService;
import com.booker.auth.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Business and platform analytics endpoints")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Returns analytics for a single business over the last 30 days.
     *
     * <p>Business owners can only view analytics for their own businesses.
     * Admins can view analytics for any business.</p>
     */
    @GetMapping("/analytics/business/{businessId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    @Operation(summary = "Business analytics — last 30 days")
    public ResponseEntity<BusinessAnalyticsResponse> getBusinessAnalytics(
            @PathVariable Long businessId,
            Authentication auth) {

        User caller = (User) auth.getPrincipal();
        return ResponseEntity.ok(
                analyticsService.getBusinessAnalytics(businessId, caller)
        );
    }

    /**
     * Platform-wide KPI summary for the admin overview dashboard.
     */
    @GetMapping("/admin/analytics/overview")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Platform overview stats (ADMIN only)")
    public ResponseEntity<PlatformOverviewResponse> getPlatformOverview() {
        return ResponseEntity.ok(analyticsService.getPlatformOverview());
    }
}
