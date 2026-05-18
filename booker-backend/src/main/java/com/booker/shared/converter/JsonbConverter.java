package com.booker.shared.converter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Shared Jackson ObjectMapper instance used by all JSONB converters.
 */
public final class JsonbConverter {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonbConverter() {}
}
