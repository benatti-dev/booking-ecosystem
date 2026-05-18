package com.booker.catalog.service;

import com.booker.catalog.dto.CreateAttributeDefinitionRequest;
import com.booker.catalog.entity.AttributeFieldType;
import com.booker.catalog.entity.ServiceAttributeDefinition;
import com.booker.shared.exception.BookerException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Validates a JSONB attributes map submitted with a service against the
 * ServiceAttributeDefinitions configured for that business category.
 *
 * Definitions are loaded via AttributeDefinitionService which is @Cacheable.
 */
@Component
@RequiredArgsConstructor
public class ServiceAttributeValidator {

    private final AttributeDefinitionService definitionService;

    /**
     * Validates the given attributes map against the definitions for the category.
     *
     * @param categoryId the business category id
     * @param attributes the attributes submitted with the service
     * @throws BookerException (400) if validation fails, containing field-level messages
     */
    public void validate(Long categoryId, Map<String, Object> attributes) {
        List<ServiceAttributeDefinition> definitions = definitionService.getDefinitions(categoryId);
        if (definitions.isEmpty()) return;

        Map<String, String> errors = new LinkedHashMap<>();
        Map<String, Object> attrs = attributes != null ? attributes : Collections.emptyMap();

        for (ServiceAttributeDefinition def : definitions) {
            String key = def.getFieldKey();
            Object value = attrs.get(key);

            // Required check
            if (def.getIsRequired() && (value == null || isBlankString(value))) {
                errors.put(key, "Field '" + def.getFieldLabel() + "' is required");
                continue;
            }
            if (value == null) continue;

            // Type check
            String typeError = checkType(def, value);
            if (typeError != null) {
                errors.put(key, typeError);
            }
        }

        if (!errors.isEmpty()) {
            String message = "Service attribute validation failed: " + errors;
            throw new BookerException(message, HttpStatus.BAD_REQUEST);
        }
    }

    private String checkType(ServiceAttributeDefinition def, Object value) {
        return switch (def.getFieldType()) {
            case TEXT -> null; // Any string accepted
            case NUMBER -> {
                if (!(value instanceof Number) && !isNumericString(value)) {
                    yield "Field '" + def.getFieldLabel() + "' must be a number";
                }
                yield null;
            }
            case BOOLEAN -> {
                if (!(value instanceof Boolean)) {
                    yield "Field '" + def.getFieldLabel() + "' must be true or false";
                }
                yield null;
            }
            case SELECT -> {
                List<String> options = def.getOptions();
                if (options != null && !options.contains(String.valueOf(value))) {
                    yield "Field '" + def.getFieldLabel() + "' must be one of: " + options;
                }
                yield null;
            }
            case MULTI_SELECT -> {
                if (!(value instanceof List<?> selected)) {
                    yield "Field '" + def.getFieldLabel() + "' must be a list";
                }
                List<String> options = def.getOptions();
                if (options != null) {
                    List<String> invalid = selected.stream()
                            .map(String::valueOf)
                            .filter(v -> !options.contains(v))
                            .toList();
                    if (!invalid.isEmpty()) {
                        yield "Field '" + def.getFieldLabel() + "' contains invalid options: " + invalid;
                    }
                }
                yield null;
            }
        };
    }

    private boolean isBlankString(Object v) {
        return v instanceof String s && s.isBlank();
    }

    private boolean isNumericString(Object v) {
        if (!(v instanceof String s)) return false;
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
