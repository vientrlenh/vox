package com.sep.vox.domain.valueobject;

import java.util.Set;

public record QuestionType(String value) {

    private static final Set<String> ALLOWED_VALUES = Set.of(
        "READ_ALOUD",
        "SHORT_ANSWER",
        "LONG_ANSWER",
        "OPINION",
        "DESCRIPTION"
    );

    public QuestionType {
        if (value == null || !ALLOWED_VALUES.contains(value)) {
            throw new IllegalArgumentException("Loại câu hỏi không hợp lệ. Chỉ chấp nhận: " + ALLOWED_VALUES);
        }
    }
}
