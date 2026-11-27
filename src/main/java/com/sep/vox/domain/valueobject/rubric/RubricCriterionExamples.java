package com.sep.vox.domain.valueobject.rubric;

import java.util.List;
import java.util.Objects;

public record RubricCriterionExamples(
    List<RubricCriterionExample> values
) {
    public RubricCriterionExamples {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("The criterion examples in the scoring framework must not be blank");
        }
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("The criterion examples in the scoring framework must not contain blank values");
        }
        values = List.copyOf(values);
    }
}
