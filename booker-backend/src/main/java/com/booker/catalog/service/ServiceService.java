package com.booker.catalog.service;

import com.booker.business.entity.Business;
import com.booker.business.repository.BusinessRepository;
import com.booker.catalog.dto.CreateServiceRequest;
import com.booker.catalog.dto.ServiceResponse;
import com.booker.catalog.entity.Service;
import com.booker.catalog.repository.ServiceRepository;
import com.booker.shared.exception.BookerException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final BusinessRepository businessRepository;
    private final ServiceAttributeValidator attributeValidator;

    @Transactional
    public ServiceResponse create(Long businessId, CreateServiceRequest req) {
        Business business = findBusinessOrThrow(businessId);

        Map<String, Object> attrs = req.attributes() != null ? req.attributes() : new HashMap<>();
        attributeValidator.validate(business.getCategory().getId(), attrs);

        Service service = Service.builder()
                .business(business)
                .category(business.getCategory())
                .name(req.name())
                .description(req.description())
                .durationMin(req.durationMin())
                .price(req.price())
                .currency(req.currency() != null ? req.currency() : "UAH")
                .attributes(attrs)
                .build();

        return toResponse(serviceRepository.save(service));
    }

    @Transactional(readOnly = true)
    public Page<ServiceResponse> listByBusiness(Long businessId, Boolean activeOnly, Pageable pageable) {
        findBusinessOrThrow(businessId);
        if (Boolean.TRUE.equals(activeOnly)) {
            return serviceRepository.findByBusinessIdAndIsActive(businessId, true, pageable)
                    .map(this::toResponse);
        }
        return serviceRepository.findByBusinessId(businessId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ServiceResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ServiceResponse update(Long businessId, Long serviceId, CreateServiceRequest req) {
        Business business = findBusinessOrThrow(businessId);
        Service service = serviceRepository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> BookerException.notFound("Service not found: " + serviceId));

        Map<String, Object> attrs = req.attributes() != null ? req.attributes() : service.getAttributes();
        attributeValidator.validate(business.getCategory().getId(), attrs);

        service.setName(req.name());
        if (req.description() != null) service.setDescription(req.description());
        service.setDurationMin(req.durationMin());
        service.setPrice(req.price());
        if (req.currency() != null) service.setCurrency(req.currency());
        service.setAttributes(attrs);

        return toResponse(serviceRepository.save(service));
    }

    @Transactional
    public void deactivate(Long businessId, Long serviceId) {
        findBusinessOrThrow(businessId);
        Service service = serviceRepository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> BookerException.notFound("Service not found: " + serviceId));
        service.setIsActive(false);
        serviceRepository.save(service);
    }

    private Service findOrThrow(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> BookerException.notFound("Service not found: " + id));
    }

    private Business findBusinessOrThrow(Long businessId) {
        return businessRepository.findByIdWithDetails(businessId)
                .orElseThrow(() -> BookerException.notFound("Business not found: " + businessId));
    }

    public ServiceResponse toResponse(Service s) {
        return new ServiceResponse(
                s.getId(),
                s.getBusiness().getId(),
                s.getCategory().getId(),
                s.getCategory().getName(),
                s.getName(),
                s.getDescription(),
                s.getDurationMin(),
                s.getPrice(),
                s.getCurrency(),
                s.getAttributes(),
                s.getIsActive(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
