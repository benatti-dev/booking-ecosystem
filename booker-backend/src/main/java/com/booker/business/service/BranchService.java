package com.booker.business.service;

import com.booker.business.dto.BranchResponse;
import com.booker.business.dto.CreateBranchRequest;
import com.booker.business.entity.Branch;
import com.booker.business.entity.Business;
import com.booker.business.repository.BranchRepository;
import com.booker.business.repository.BusinessRepository;
import com.booker.shared.exception.BookerException;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private static final GeometryFactory GEO_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final BranchRepository branchRepository;
    private final BusinessRepository businessRepository;

    @Transactional(readOnly = true)
    public List<BranchResponse> listByBusiness(Long businessId) {
        findBusinessOrThrow(businessId);
        return branchRepository.findByBusinessId(businessId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public BranchResponse create(Long businessId, CreateBranchRequest req) {
        Business business = findBusinessOrThrow(businessId);

        Point location = null;
        if (req.latitude() != null && req.longitude() != null) {
            location = GEO_FACTORY.createPoint(new Coordinate(req.longitude(), req.latitude()));
        }

        Branch branch = Branch.builder()
                .business(business)
                .name(req.name())
                .address(req.address())
                .city(req.city())
                .country(req.country() != null ? req.country() : "UA")
                .postalCode(req.postalCode())
                .location(location)
                .phone(req.phone())
                .email(req.email())
                .timezone(req.timezone() != null ? req.timezone() : "Europe/Kiev")
                .isPrimary(req.isPrimary() != null ? req.isPrimary() : false)
                .build();

        return toResponse(branchRepository.save(branch));
    }

    @Transactional
    public BranchResponse update(Long businessId, Long branchId, CreateBranchRequest req) {
        findBusinessOrThrow(businessId);
        Branch branch = branchRepository.findByIdAndBusinessId(branchId, businessId)
                .orElseThrow(() -> BookerException.notFound("Branch not found: " + branchId));

        branch.setName(req.name());
        branch.setAddress(req.address());
        branch.setCity(req.city());
        if (req.country() != null) branch.setCountry(req.country());
        branch.setPostalCode(req.postalCode());
        branch.setPhone(req.phone());
        branch.setEmail(req.email());
        if (req.timezone() != null) branch.setTimezone(req.timezone());
        if (req.isPrimary() != null) branch.setIsPrimary(req.isPrimary());

        if (req.latitude() != null && req.longitude() != null) {
            branch.setLocation(GEO_FACTORY.createPoint(
                    new Coordinate(req.longitude(), req.latitude())));
        }

        return toResponse(branchRepository.save(branch));
    }

    @Transactional
    public void delete(Long businessId, Long branchId) {
        findBusinessOrThrow(businessId);
        Branch branch = branchRepository.findByIdAndBusinessId(branchId, businessId)
                .orElseThrow(() -> BookerException.notFound("Branch not found: " + branchId));
        branchRepository.delete(branch);
    }

    private Business findBusinessOrThrow(Long businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> BookerException.notFound("Business not found: " + businessId));
    }

    public BranchResponse toResponse(Branch b) {
        Double lat = b.getLocation() != null ? b.getLocation().getY() : null;
        Double lng = b.getLocation() != null ? b.getLocation().getX() : null;
        return new BranchResponse(
                b.getId(),
                b.getBusiness().getId(),
                b.getName(),
                b.getAddress(),
                b.getCity(),
                b.getCountry(),
                b.getPostalCode(),
                lat, lng,
                b.getPhone(),
                b.getEmail(),
                b.getTimezone(),
                b.getIsPrimary(),
                b.getStatus(),
                b.getCreatedAt()
        );
    }
}
