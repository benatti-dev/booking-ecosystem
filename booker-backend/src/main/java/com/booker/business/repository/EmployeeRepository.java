package com.booker.business.repository;

import com.booker.business.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Page<Employee> findByBusinessId(Long businessId, Pageable pageable);

    List<Employee> findByBusinessIdAndIsActiveTrue(Long businessId);

    Optional<Employee> findByIdAndBusinessId(Long id, Long businessId);
}
