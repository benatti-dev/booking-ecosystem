package com.booker.business.repository;

import com.booker.business.entity.BookableResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends JpaRepository<BookableResource, Long> {

    Page<BookableResource> findByBusinessId(Long businessId, Pageable pageable);

    List<BookableResource> findByBusinessIdAndIsActiveTrue(Long businessId);

    Optional<BookableResource> findByIdAndBusinessId(Long id, Long businessId);
}
