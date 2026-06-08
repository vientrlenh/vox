package com.sep.vox.application.common;

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
}
