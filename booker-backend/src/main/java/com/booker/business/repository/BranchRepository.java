package com.booker.business.repository;

import com.booker.business.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    List<Branch> findByBusinessId(Long businessId);

    Optional<Branch> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByBusinessIdAndIsPrimaryTrue(Long businessId);
}
