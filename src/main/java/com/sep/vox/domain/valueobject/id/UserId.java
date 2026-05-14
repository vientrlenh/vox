package com.sep.vox.domain.valueobject.id;

import java.util.Objects;
import java.util.UUID;

public record UserId(
    UUID value
) {
    public UserId {
        Objects.requireNonNull(value);
    }
}
