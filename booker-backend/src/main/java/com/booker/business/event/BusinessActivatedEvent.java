package com.booker.business.event;

import com.booker.business.entity.Business;

/**
 * Published when a business transitions from PENDING → ACTIVE.
 * Consumed by the Notification module (Phase 4).
 */
public record BusinessActivatedEvent(Business business) {}
