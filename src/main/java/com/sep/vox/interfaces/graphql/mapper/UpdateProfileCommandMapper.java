package com.sep.vox.interfaces.graphql.mapper;

import java.time.LocalDate;
import java.util.Map;

import com.sep.vox.application.port.input.command.user.UpdateProfileCommand;

/**
 * Takes a raw {@code Map} instead of a record: that is the ONLY way to tell "the client did not send the
 * field" from "the client sent null" in GraphQL -- once a record is bound, both cases come out as null.
 * The pair {@code containsKey} + value is what gives PATCH semantics, and thanks to it
 * {@code avatarUrl: null} means REMOVE the avatar instead of being ignored.
 *
 * <p>{@code dateOfBirth} arrives already coerced by the {@code Date} scalar, so it is a
 * {@link LocalDate} or null and needs no parsing here.
 *
 * <p>Same shape as {@link UpdateSchoolUserCommandMapper} -- when you change one, look at the other.
 */
public final class UpdateProfileCommandMapper {

    private UpdateProfileCommandMapper() {
    }

    public static UpdateProfileCommand fromInput(Map<String, Object> input) {
        return new UpdateProfileCommand(
            valueOf(input.get("fullName")),
            input.containsKey("fullName"),
            valueOf(input.get("phone")),
            input.containsKey("phone"),
            valueOf(input.get("address")),
            input.containsKey("address"),
            (LocalDate) input.get("dateOfBirth"),
            input.containsKey("dateOfBirth"),
            valueOf(input.get("avatarUrl")),
            input.containsKey("avatarUrl")
        );
    }

    private static String valueOf(Object value) {
        return value == null ? null : value.toString();
    }
}
