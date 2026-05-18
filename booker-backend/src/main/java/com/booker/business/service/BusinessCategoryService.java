package com.booker.business.service;

import com.booker.business.dto.CategoryResponse;
import com.booker.business.dto.CreateCategoryRequest;
import com.booker.business.entity.BusinessCategory;
import com.booker.business.repository.BusinessCategoryRepository;
import com.booker.shared.exception.BookerException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessCategoryService {

    private final BusinessCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest req) {
        if (categoryRepository.existsByName(req.name())) {
            throw BookerException.conflict("Category with name '" + req.name() + "' already exists");
        }
        BusinessCategory category = BusinessCategory.builder()
                .name(req.name())
                .label(req.label())
                .iconUrl(req.iconUrl())
                .resourceType(req.resourceType())
                .build();
        return toResponse(categoryRepository.save(category));
    }

    public BusinessCategory findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> BookerException.notFound("Category not found: " + id));
    }

    public CategoryResponse toResponse(BusinessCategory c) {
        return new CategoryResponse(
                c.getId(), c.getName(), c.getLabel(),
                c.getIconUrl(), c.getResourceType().name());
    }
}
