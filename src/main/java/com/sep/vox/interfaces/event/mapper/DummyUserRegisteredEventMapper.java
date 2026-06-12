package com.sep.vox.interfaces.event.mapper;

import com.fasterxml.jackson.databind.JsonNode;

public final class DummyUserRegisteredEventMapper {

    private DummyUserRegisteredEventMapper() {}

    public static UserRegisteredPayload fromJson(JsonNode payload) {
        return new UserRegisteredPayload(
            textOf(payload, "userId"),
            textOf(payload, "email"),
            textOf(payload, "fullName")
        );
    }

    private static String textOf(JsonNode node, String field) {
        if (node == null) return null;
        var child = node.get(field);
        return child == null || child.isNull() ? null : child.asText();
    }

    public record UserRegisteredPayload(
        String userId,
        String email,
        String fullName
    ) {}
}
