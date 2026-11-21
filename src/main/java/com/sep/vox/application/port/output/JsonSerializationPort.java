package com.sep.vox.application.port.output;

import java.util.List;
import java.util.Map;

public interface JsonSerializationPort {
    String toJson(Object value);

    Map<String, String> toStringMap(String json);

    List<String> toStringList(String json);

    List<Map<String, String>> toStringMapList(String json);

    /** Reads the string array stored in {@code field} of a JSON object, e.g. {@code {"current":[...]}}. */
    List<String> toStringListField(String json, String field);

    /**
     * Parses a JSON object into {@code targetType}, ignoring unknown properties. Throws
     * {@code IllegalStateException} when the JSON is malformed or does not bind.
     */
    <T> T fromJson(String json, Class<T> targetType);

    /**
     * Parses a JSON array into a list of {@code elementType} and throws {@code IllegalStateException} when the JSON
     * is malformed or does not bind (including unknown properties). Returns {@code null} for the JSON text
     * {@code null}.
     */
    <T> List<T> fromJsonList(String json, Class<T> elementType);

    /**
     * Reads the text of {@code field} in a JSON object. Returns {@code null} when the JSON is blank or the field is
     * absent or null; malformed JSON throws an unchecked exception.
     */
    String toStringField(String json, String field);

    /**
     * Binds a raw value (usually a {@code Map}) to {@code targetType}.
     *
     * <p>The three methods below bind with default Jackson settings, not with the application's configured mapper
     * that the methods above use (which, for example, reads enums case-insensitively). An invalid value makes them
     * throw with the binding error as the message, so a caller can pass it back to the client.
     */
    <T> T convertValue(Object value, Class<T> targetType);

    /** Converts any object (e.g. a value object) to a raw {@code Map} of its JSON properties. */
    Map<String, Object> convertToObjectMap(Object value);

    /** Parses a JSON object into a raw {@code Map}. */
    Map<String, Object> toObjectMap(String json);
}
