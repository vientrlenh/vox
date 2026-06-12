package com.sep.vox.domain.valueobject.rubric;

import java.util.List;
import java.util.Objects;

public record RubricCriterionExamples(
    List<RubricCriterionExample> values
) {
    public RubricCriterionExamples {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Ví dụ của tiêu chí trong khung chấm điểm không được để trống");
        }
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Ví dụ của tiêu chí trong khung chấm điểm không được chứa giá trị trống");
        }
        values = List.copyOf(values);
    }
}
