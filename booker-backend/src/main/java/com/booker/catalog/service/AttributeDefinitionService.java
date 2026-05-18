package com.booker.catalog.service;

import com.booker.catalog.dto.CreateAttributeDefinitionRequest;
import com.booker.catalog.dto.AttributeDefinitionResponse;
import com.booker.catalog.entity.ServiceAttributeDefinition;
import com.booker.catalog.repository.ServiceAttributeDefinitionRepository;
import com.booker.business.entity.BusinessCategory;
import com.booker.business.service.BusinessCategoryService;
import com.booker.shared.exception.BookerException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttributeDefinitionService {

    private final ServiceAttributeDefinitionRepository definitionRepository;
    private final BusinessCategoryService categoryService;

    @Cacheable(value = "attributeDefs", key = "#categoryId")
    @Transactional(readOnly = true)
    public List<ServiceAttributeDefinition> getDefinitions(Long categoryId) {
        return definitionRepository.findByCategoryIdOrderBySortOrder(categoryId);
    }

    @Transactional(readOnly = true)
    public List<AttributeDefinitionResponse> getDefinitionResponses(Long categoryId) {
        // Validate category exists
        categoryService.getById(categoryId);
        return getDefinitions(categoryId).stream().map(this::toResponse).toList();
    }

    @CacheEvict(value = "attributeDefs", key = "#categoryId")
    @Transactional
    public AttributeDefinitionResponse create(Long categoryId, CreateAttributeDefinitionRequest req) {
        BusinessCategory category = categoryService.findOrThrow(categoryId);

        ServiceAttributeDefinition def = ServiceAttributeDefinition.builder()
                .category(category)
                .fieldKey(req.fieldKey())
                .fieldLabel(req.fieldLabel())
                .fieldType(req.fieldType())
                .options(req.options())
                .isRequired(req.isRequired() != null ? req.isRequired() : false)
                .sortOrder(req.sortOrder() != null ? req.sortOrder() : 0)
                .build();

        return toResponse(definitionRepository.save(def));
    }

    @CacheEvict(value = "attributeDefs", key = "#categoryId")
    @Transactional
    public void delete(Long categoryId, Long definitionId) {
        ServiceAttributeDefinition def = definitionRepository.findById(definitionId)
                .orElseThrow(() -> BookerException.notFound("Attribute definition not found: " + definitionId));
        if (!def.getCategory().getId().equals(categoryId)) {
            throw BookerException.badRequest("Definition does not belong to category " + categoryId);
        }
        definitionRepository.delete(def);
    }

    public AttributeDefinitionResponse toResponse(ServiceAttributeDefinition d) {
        return new AttributeDefinitionResponse(
                d.getId(), d.getCategory().getId(),
                d.getFieldKey(), d.getFieldLabel(), d.getFieldType(),
                d.getOptions(), d.getIsRequired(), d.getSortOrder());
    }
}
