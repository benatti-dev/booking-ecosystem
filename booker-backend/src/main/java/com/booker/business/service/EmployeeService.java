package com.booker.business.service;

import com.booker.auth.entity.User;
import com.booker.auth.entity.UserRole;
import com.booker.business.dto.CreateEmployeeRequest;
import com.booker.business.dto.EmployeeResponse;
import com.booker.business.entity.Branch;
import com.booker.business.entity.Business;
import com.booker.business.entity.Employee;
import com.booker.business.repository.BranchRepository;
import com.booker.business.repository.BusinessRepository;
import com.booker.business.repository.EmployeeRepository;
import com.booker.shared.exception.BookerException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final BusinessRepository businessRepository;
    private final BranchRepository branchRepository;

    @Transactional
    public EmployeeResponse create(Long businessId, CreateEmployeeRequest req, Authentication auth) {
        Business business = findBusinessOrThrow(businessId);
        checkOwnerOrAdmin(business, auth);

        Branch branch = null;
        if (req.branchId() != null) {
            branch = branchRepository.findByIdAndBusinessId(req.branchId(), businessId)
                    .orElseThrow(() -> BookerException.notFound("Branch not found: " + req.branchId()));
        }

        Employee employee = Employee.builder()
                .business(business)
                .branch(branch)
                .displayName(req.displayName())
                .bio(req.bio())
                .avatarUrl(req.avatarUrl())
                .position(req.position())
                .build();

        return toResponse(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> listByBusiness(Long businessId, Pageable pageable) {
        findBusinessOrThrow(businessId);
        return employeeRepository.findByBusinessId(businessId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long businessId, Long employeeId) {
        return toResponse(findOrThrow(businessId, employeeId));
    }

    @Transactional
    public EmployeeResponse deactivate(Long businessId, Long employeeId, Authentication auth) {
        Business business = findBusinessOrThrow(businessId);
        checkOwnerOrAdmin(business, auth);
        Employee employee = findOrThrow(businessId, employeeId);
        employee.setActive(false);
        return toResponse(employeeRepository.save(employee));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Business findBusinessOrThrow(Long businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> BookerException.notFound("Business not found: " + businessId));
    }

    private Employee findOrThrow(Long businessId, Long employeeId) {
        return employeeRepository.findByIdAndBusinessId(employeeId, businessId)
                .orElseThrow(() -> BookerException.notFound("Employee not found: " + employeeId));
    }

    private void checkOwnerOrAdmin(Business business, Authentication auth) {
        User actor = (User) auth.getPrincipal();
        if (actor.getRole() == UserRole.ADMIN) return;
        if (!business.getOwner().getId().equals(actor.getId())) {
            throw BookerException.forbidden("Access denied");
        }
    }

    public EmployeeResponse toResponse(Employee e) {
        return new EmployeeResponse(
                e.getId(),
                e.getUser() != null ? e.getUser().getId() : null,
                e.getBusiness().getId(),
                e.getBranch() != null ? e.getBranch().getId() : null,
                e.getDisplayName(),
                e.getBio(),
                e.getAvatarUrl(),
                e.getPosition(),
                e.isActive(),
                e.getCreatedAt()
        );
    }
}
