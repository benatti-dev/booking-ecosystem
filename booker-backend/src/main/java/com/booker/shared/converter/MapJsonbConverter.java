package com.booker.shared.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.util.HashMap;
import java.util.Map;

/** JPA converter for JSONB ↔ Map<String, Object>. */
@Converter
public class MapJsonbConverter implements AttributeConverter<Map<String, Object>, Object> {

    @Override
    public Object convertToDatabaseColumn(Map<String, Object> attribute) {
        try {
            PGobject pg = new PGobject();
            pg.setType("jsonb");
            pg.setValue(attribute == null ? "{}" : JsonbConverter.MAPPER.writeValueAsString(attribute));
            return pg;
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot serialize map to JSON", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(Object dbData) {
        if (dbData == null) return new HashMap<>();
        String json = dbData instanceof PGobject pg ? pg.getValue() : dbData.toString();
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return JsonbConverter.MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot deserialize JSON to Map: " + json, e);
        }
    }
}

