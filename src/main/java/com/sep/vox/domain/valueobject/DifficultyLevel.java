package com.sep.vox.domain.valueobject;

import java.util.Set;

public record DifficultyLevel(String value) {

    private static final Set<String> ALLOWED_VALUES = Set.of(
        "A1", "A2", "B1", "B2", "C1", "C2"
    );

    public DifficultyLevel {
        if (value == null || !ALLOWED_VALUES.contains(value)) {
            throw new IllegalArgumentException("Mức độ khó không hợp lệ. Chỉ chấp nhận: " + ALLOWED_VALUES);
        }
    }
}
