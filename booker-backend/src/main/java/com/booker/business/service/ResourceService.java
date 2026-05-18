package com.booker.business.service;

import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.business.dto.CreateResourceRequest;
import com.booker.business.dto.ResourceResponse;
import com.booker.business.entity.BookableResource;
import com.booker.business.entity.Branch;
import com.booker.business.entity.Business;
import com.booker.business.repository.BranchRepository;
import com.booker.business.repository.BusinessRepository;
import com.booker.business.repository.ResourceRepository;
import com.booker.shared.exception.BookerException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final BusinessRepository businessRepository;
    private final BranchRepository branchRepository;

    @Transactional
    public ResourceResponse create(Long businessId, CreateResourceRequest req, Authentication auth) {
        Business business = findBusinessOrThrow(businessId);
        checkOwnerOrAdmin(business, auth);

        Branch branch = branchRepository.findByIdAndBusinessId(req.branchId(), businessId)
                .orElseThrow(() -> BookerException.notFound("Branch not found: " + req.branchId()));

        BookableResource resource = BookableResource.builder()
                .business(business)
                .branch(branch)
                .name(req.name())
                .resourceType(req.resourceType())
                .capacity(req.capacity())
                .meta(req.meta() != null ? req.meta() : new java.util.HashMap<>())
                .build();

        return toResponse(resourceRepository.save(resource));
    }

    @Transactional(readOnly = true)
    public Page<ResourceResponse> listByBusiness(Long businessId, Pageable pageable) {
        findBusinessOrThrow(businessId);
        return resourceRepository.findByBusinessId(businessId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getById(Long businessId, Long resourceId) {
        return toResponse(findOrThrow(businessId, resourceId));
    }

    @Transactional
    public ResourceResponse deactivate(Long businessId, Long resourceId, Authentication auth) {
        Business business = findBusinessOrThrow(businessId);
        checkOwnerOrAdmin(business, auth);
        BookableResource resource = findOrThrow(businessId, resourceId);
        resource.setActive(false);
        return toResponse(resourceRepository.save(resource));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Business findBusinessOrThrow(Long businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> BookerException.notFound("Business not found: " + businessId));
    }

    private BookableResource findOrThrow(Long businessId, Long resourceId) {
        return resourceRepository.findByIdAndBusinessId(resourceId, businessId)
                .orElseThrow(() -> BookerException.notFound("Resource not found: " + resourceId));
    }

    private void checkOwnerOrAdmin(Business business, Authentication auth) {
        User actor = (User) auth.getPrincipal();
        if (actor.getRole() == UserRole.ADMIN) return;
        if (!business.getOwner().getId().equals(actor.getId())) {
            throw BookerException.forbidden("Access denied");
        }
    }

    public ResourceResponse toResponse(BookableResource r) {
        return new ResourceResponse(
                r.getId(),
                r.getBusiness().getId(),
                r.getBranch().getId(),
                r.getName(),
                r.getResourceType(),
                r.getCapacity(),
                r.getMeta(),
                r.isActive()
        );
    }
}
