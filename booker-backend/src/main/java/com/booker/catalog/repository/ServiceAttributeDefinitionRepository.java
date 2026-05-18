package com.booker.catalog.repository;

import com.booker.catalog.entity.ServiceAttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceAttributeDefinitionRepository extends JpaRepository<ServiceAttributeDefinition, Long> {

    List<ServiceAttributeDefinition> findByCategoryIdOrderBySortOrder(Long categoryId);
}
