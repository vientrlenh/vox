package com.sep.vox.application.query.dto;

import com.sep.vox.domain.model.user.UserStatus;
import java.util.UUID;

/**
 * A student / teacher shown in the assessment directory.
 *
 * <p>Deliberately flat (fullName and email sit directly here instead of nesting the `User` type): the
 * directory surface must be a leaf branch in the graph, opening no extra edge for the TEACHER role --
 * the same reason as written in `myclass.graphqls`.
 */
public record AssessmentDirectoryUserInfo(
    UUID userId,
    String fullName,
    String email,
    UserStatus status
) {
    // Auxiliary constructor for the JPQL "SELECT NEW ...AssessmentDirectoryUserInfo(...)" projections: the status is a plain String
    // column on the JPA entity, so this overload takes the raw string and converts it here, keeping the record itself enum-typed.
    public AssessmentDirectoryUserInfo(UUID userId, String fullName, String email, String status) {
        this(userId, fullName, email, UserStatus.valueOf(status));
    }
}
