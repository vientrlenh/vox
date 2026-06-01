package com.sep.vox.infrastructure.persistence.mapper;

import tools.jackson.databind.ObjectMapper;

final class JsonValueObjectMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonValueObjectMapper() {}

    static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize value object to JSON", exception);
        }
    }

    static <T> T fromJson(String json, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not deserialize value object from JSON", exception);
        }
    }
}
