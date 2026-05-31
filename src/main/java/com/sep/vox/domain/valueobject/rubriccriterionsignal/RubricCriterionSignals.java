package com.sep.vox.domain.valueobject.rubriccriterionsignal;

import java.util.List;
import java.util.Objects;

public record RubricCriterionSignals(
    List<RubricCriterionSignal> values
) {
    public RubricCriterionSignals {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Các dấu hiệu không được để trống");
        }
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Các dấu hiệu không được chứa phần tử trống");
        }
        values = List.copyOf(values);
    }
}
