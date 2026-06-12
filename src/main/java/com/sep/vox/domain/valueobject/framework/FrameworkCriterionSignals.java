package com.sep.vox.domain.valueobject.framework;

import java.util.List;
import java.util.Objects;

public record FrameworkCriterionSignals(
    List<FrameworkCriterionSignal> values
) {
    public FrameworkCriterionSignals {
        if (values != null && values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Các dấu hiệu không được chứa phần tử trống");
        }
        values = List.copyOf(values);
    }
}
