package com.sep.vox.infrastructure.persistence.mapper;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

final class JsonValueObjectMapper {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();

    static String toJson(Object value) {
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi khi serialize giá trị sang json", e);
        }
    }

    static <T> T fromJson(String json, Class<T> type) {
        if (json == null) return null;
        try {
            return JSON_MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi khi deserialize giá trị json sang lớp", e);
        }
    }

    static <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null) return null;
        try {
            return JSON_MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi khi deserialize giá trị json sang kiểu", e);
        }
    }
}
