package com.sep.vox.domain.valueobject.id;

import java.util.Objects;
import java.util.UUID;

public record RegisterFormId(UUID value) {
    public RegisterFormId {
        Objects.requireNonNull(value);
    }
}
