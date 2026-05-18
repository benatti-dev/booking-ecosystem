package com.booker.catalog.repository;

import com.booker.catalog.entity.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    Page<Service> findByBusinessIdAndIsActive(Long businessId, Boolean isActive, Pageable pageable);

    Page<Service> findByBusinessId(Long businessId, Pageable pageable);

    Optional<Service> findByIdAndBusinessId(Long id, Long businessId);

    @Query(value = """
            SELECT s.* FROM services s
            WHERE s.business_id = :businessId
              AND s.is_active = true
              AND s.search_vector @@ plainto_tsquery('simple', :query)
            ORDER BY ts_rank(s.search_vector, plainto_tsquery('simple', :query)) DESC
            """, nativeQuery = true)
    Page<Service> fullTextSearch(
            @Param("businessId") Long businessId,
            @Param("query") String query,
            Pageable pageable);
}
