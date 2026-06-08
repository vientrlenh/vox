package com.sep.vox.application.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.ObjectMapper;

public final class JsonSerialization {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonSerialization() {
    }

    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize value to JSON", exception);
        }
    }

    public static Map<String, String> toStringMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<?, ?> raw = OBJECT_MAPPER.readValue(json, Map.class);
            var result = new LinkedHashMap<String, String>();
            raw.forEach((key, value) -> result.put(
                key == null ? null : key.toString(),
                value == null ? null : value.toString()
            ));
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not deserialize JSON to string map", exception);
        }
    }

    public static List<String> toStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<?> raw = OBJECT_MAPPER.readValue(json, List.class);
            return raw.stream()
                .map(value -> value == null ? null : value.toString())
                .toList();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not deserialize JSON to string list", exception);
        }
    }

    public static List<Map<String, String>> toStringMapList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<?> raw = OBJECT_MAPPER.readValue(json, List.class);
            return raw.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(JsonSerialization::toStringMap)
                .toList();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not deserialize JSON to string map list", exception);
        }
    }

    private static Map<String, String> toStringMap(Map<?, ?> raw) {
        var result = new LinkedHashMap<String, String>();
        raw.forEach((key, value) -> result.put(
            key == null ? null : key.toString(),
            value == null ? null : value.toString()
        ));
        return result;
    }
}
