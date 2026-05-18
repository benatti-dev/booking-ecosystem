package com.booker.shared.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.util.ArrayList;
import java.util.List;

/** JPA converter for JSONB ↔ List<String>. */
@Converter
public class ListStringJsonbConverter implements AttributeConverter<List<String>, Object> {

    @Override
    public Object convertToDatabaseColumn(List<String> attribute) {
        try {
            PGobject pg = new PGobject();
            pg.setType("jsonb");
            pg.setValue(attribute == null ? "[]" : JsonbConverter.MAPPER.writeValueAsString(attribute));
            return pg;
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot serialize list to JSON", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(Object dbData) {
        if (dbData == null) return new ArrayList<>();
        String json = dbData instanceof PGobject pg ? pg.getValue() : dbData.toString();
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return JsonbConverter.MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot deserialize JSON to List: " + json, e);
        }
    }
}
