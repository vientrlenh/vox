package com.sep.vox.application.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sep.vox.application.port.output.JsonSerializationPort;

import tools.jackson.databind.ObjectMapper;

public class FakeJsonSerializationPort implements JsonSerializationPort {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize value to JSON", exception);
        }
    }

    @Override
    public Map<String, String> toStringMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<?, ?> raw = objectMapper.readValue(json, Map.class);
            return toStringMap(raw);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not deserialize JSON to string map", exception);
        }
    }

    @Override
    public List<String> toStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<?> raw = objectMapper.readValue(json, List.class);
            return raw.stream()
                .map(value -> value == null ? null : value.toString())
                .toList();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not deserialize JSON to string list", exception);
        }
    }

    @Override
    public List<Map<String, String>> toStringMapList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<?> raw = objectMapper.readValue(json, List.class);
            return raw.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(this::toStringMap)
                .toList();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not deserialize JSON to string map list", exception);
        }
    }

    private Map<String, String> toStringMap(Map<?, ?> raw) {
        var result = new LinkedHashMap<String, String>();
        raw.forEach((key, value) -> result.put(
            key == null ? null : key.toString(),
            value == null ? null : value.toString()
        ));
        return result;
    }
}
