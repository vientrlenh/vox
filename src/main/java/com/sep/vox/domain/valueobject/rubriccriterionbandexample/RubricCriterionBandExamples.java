package com.sep.vox.domain.valueobject.rubriccriterionbandexample;

import java.util.List;
import java.util.Objects;

public record RubricCriterionBandExamples(
    List<RubricCriterionBandExample> values
) {
    public RubricCriterionBandExamples {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Ví dụ của band không được để trống");
        }
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Ví dụ của band không được chứa giá trị trống");
        }
        values = List.copyOf(values);
    }
}
