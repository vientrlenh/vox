package com.sep.vox.domain.valueobject.framework;

import java.util.List;
import java.util.Objects;

public record FrameworkCriterionSignals(
    List<FrameworkCriterionSignal> values
) {
    public FrameworkCriterionSignals {
        if (values == null) values = List.of();
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("The signals must not contain blank elements");
        }
        values = List.copyOf(values);
    }
}
