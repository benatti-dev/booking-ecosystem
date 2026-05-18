package com.booker.business.service;

import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.business.dto.*;
import com.booker.business.entity.*;
import com.booker.business.event.BusinessActivatedEvent;
import com.booker.business.repository.BusinessCategoryRepository;
import com.booker.business.repository.BusinessRepository;
import com.booker.shared.exception.BookerException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessCategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ── Create ────────────────────────────────────────────────────

    @Transactional
    public BusinessResponse create(CreateBusinessRequest req, User owner) {
        BusinessCategory category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> BookerException.notFound("Category not found: " + req.categoryId()));

        Business business = Business.builder()
                .owner(owner)
                .category(category)
                .name(req.name())
                .description(req.description())
                .logoUrl(req.logoUrl())
                .meta(req.meta() != null ? req.meta() : new java.util.HashMap<>())
                .build();

        return toResponse(businessRepository.save(business));
    }

    // ── Read ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BusinessResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<BusinessResponse> getMyBusinesses(Long ownerId, Pageable pageable) {
        return businessRepository.findByOwnerId(ownerId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<BusinessResponse> getByStatus(BusinessStatus status, Pageable pageable) {
        return businessRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    // ── Update ────────────────────────────────────────────────────

    @Transactional
    public BusinessResponse update(Long id, UpdateBusinessRequest req, Authentication auth) {
        Business business = findOrThrow(id);
        checkOwnerOrAdmin(business, auth);

        if (req.name() != null) business.setName(req.name());
        if (req.description() != null) business.setDescription(req.description());
        if (req.logoUrl() != null) business.setLogoUrl(req.logoUrl());
        if (req.meta() != null) business.setMeta(req.meta());

        return toResponse(businessRepository.save(business));
    }

    // ── Status Transition (ADMIN only) ────────────────────────────

    @Transactional
    public BusinessResponse changeStatus(Long id, BusinessStatusRequest req, Authentication auth) {
        User actor = (User) auth.getPrincipal();
        if (actor.getRole() != UserRole.ADMIN) {
            throw BookerException.forbidden("Only admins can change business status");
        }

        Business business = findOrThrow(id);
        BusinessStatus newStatus = req.status();
        validateTransition(business.getStatus(), newStatus);

        business.setStatus(newStatus);
        Business saved = businessRepository.save(business);

        if (newStatus == BusinessStatus.ACTIVE) {
            eventPublisher.publishEvent(new BusinessActivatedEvent(saved));
        }

        return toResponse(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Business findOrThrow(Long id) {
        return businessRepository.findByIdWithDetails(id)
                .orElseThrow(() -> BookerException.notFound("Business not found: " + id));
    }

    private void checkOwnerOrAdmin(Business business, Authentication auth) {
        User actor = (User) auth.getPrincipal();
        if (actor.getRole() == UserRole.ADMIN) return;
        if (!business.getOwner().getId().equals(actor.getId())) {
            throw BookerException.forbidden("You do not own this business");
        }
    }

    private void validateTransition(BusinessStatus current, BusinessStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == BusinessStatus.ACTIVE || next == BusinessStatus.REJECTED;
            case ACTIVE -> next == BusinessStatus.SUSPENDED;
            case SUSPENDED -> next == BusinessStatus.ACTIVE;
            case REJECTED -> false;
        };
        if (!valid) {
            throw BookerException.badRequest(
                    "Invalid status transition: " + current + " → " + next);
        }
    }

    public BusinessResponse toResponse(Business b) {
        return new BusinessResponse(
                b.getId(),
                b.getOwner().getId(),
                b.getOwner().getFullName(),
                new BusinessResponse.CategorySummary(
                        b.getCategory().getId(),
                        b.getCategory().getName(),
                        b.getCategory().getLabel(),
                        b.getCategory().getResourceType().name()),
                b.getName(),
                b.getDescription(),
                b.getLogoUrl(),
                b.getStatus(),
                b.getMeta(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}
