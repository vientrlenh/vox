package com.sep.vox.application.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sep.vox.application.port.output.JsonSerializationPort;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class FakeJsonSerializationPort implements JsonSerializationPort {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectMapper unknownPropertyRejectingMapper = JsonMapper.builder()
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();

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

    @Override
    public List<String> toStringListField(String json, String field) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            List<String> result = new java.util.ArrayList<String>();
            for (JsonNode node : root.path(field)) {
                if (!node.asString().isBlank()) {
                    result.add(node.asString());
                }
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not deserialize JSON field to string list", exception);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> targetType) {
        try {
            return objectMapper.readValue(json, targetType);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not deserialize JSON to " + targetType.getSimpleName(), exception);
        }
    }

    @Override
    public <T> List<T> fromJsonList(String json, Class<T> elementType) {
        try {
            return unknownPropertyRejectingMapper.readValue(json,
                unknownPropertyRejectingMapper.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not deserialize JSON to list", exception);
        }
    }

    @Override
    public String toStringField(String json, String field) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return objectMapper.readTree(json).path(field).asString(null);
    }

    @Override
    public <T> T convertValue(Object value, Class<T> targetType) {
        return objectMapper.convertValue(value, targetType);
    }

    @Override
    public Map<String, Object> convertToObjectMap(Object value) {
        return objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {
        });
    }

    @Override
    public Map<String, Object> toObjectMap(String json) {
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
    }

    private Map<String, String> toStringMap(Map<?, ?> raw) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        raw.forEach((key, value) -> result.put(
            key == null ? null : key.toString(),
            value == null ? null : value.toString()
        ));
        return result;
    }
}
