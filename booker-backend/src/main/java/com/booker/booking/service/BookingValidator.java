package com.booker.booking.service;

import com.booker.booking.dto.CreateBookingRequest;
import com.booker.business.entity.Business;
import com.booker.business.entity.BusinessStatus;
import com.booker.business.entity.Employee;
import com.booker.business.repository.BusinessRepository;
import com.booker.business.repository.EmployeeRepository;
import com.booker.catalog.entity.Service;
import com.booker.catalog.repository.ServiceRepository;
import com.booker.shared.exception.BookerException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingValidator {

    private final BusinessRepository businessRepository;
    private final ServiceRepository serviceRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Validates that the booking request is coherent before acquiring the lock.
     *
     * @return the loaded Service entity (to avoid re-fetching)
     */
    public Service validate(CreateBookingRequest req) {
        // Validate service
        Service service = serviceRepository.findById(req.serviceId())
                .orElseThrow(() -> BookerException.notFound("Service not found: " + req.serviceId()));

        if (!Boolean.TRUE.equals(service.getIsActive())) {
            throw BookerException.badRequest("Service is not active");
        }

        // Validate business
        Business business = businessRepository.findById(service.getBusiness().getId())
                .orElseThrow(() -> BookerException.notFound("Business not found"));

        if (business.getStatus() != BusinessStatus.ACTIVE) {
            throw BookerException.badRequest("Business is not active");
        }

        // Validate employee assignment (if employee booking)
        if (req.employeeId() != null) {
            Employee employee = employeeRepository.findById(req.employeeId())
                    .orElseThrow(() -> BookerException.notFound("Employee not found: " + req.employeeId()));

            if (!employee.isActive()) {
                throw BookerException.badRequest("Employee is not active");
            }

            if (!employee.getBusiness().getId().equals(business.getId())) {
                throw BookerException.badRequest("Employee does not belong to this business");
            }
        }

        if (req.employeeId() == null && req.resourceId() == null) {
            throw BookerException.badRequest("Either employeeId or resourceId must be provided");
        }

        return service;
    }
}
