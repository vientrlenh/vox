package com.sep.vox.domain.valueobject.id;

import java.util.Objects;
import java.util.UUID;

public record RoleId(
    UUID value
) {
    public RoleId {
        Objects.requireNonNull(value);
    }
}
