package com.booker.business.repository;

import com.booker.business.entity.BusinessCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessCategoryRepository extends JpaRepository<BusinessCategory, Long> {

    boolean existsByName(String name);

    Optional<BusinessCategory> findByName(String name);
}
