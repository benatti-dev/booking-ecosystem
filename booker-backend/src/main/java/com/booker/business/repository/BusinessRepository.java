package com.booker.business.repository;

import com.booker.business.entity.Business;
import com.booker.business.entity.BusinessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    @Query("SELECT b FROM Business b JOIN FETCH b.category JOIN FETCH b.owner WHERE b.id = :id")
    Optional<Business> findByIdWithDetails(@Param("id") Long id);

    Page<Business> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Business> findByStatus(BusinessStatus status, Pageable pageable);

    List<Business> findByOwnerIdAndStatus(Long ownerId, BusinessStatus status);

    boolean existsByOwnerIdAndId(Long ownerId, Long id);
}
