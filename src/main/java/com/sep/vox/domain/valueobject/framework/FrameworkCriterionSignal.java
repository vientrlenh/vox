package com.sep.vox.domain.valueobject.framework;

public record FrameworkCriterionSignal(
    String code,
    String description,
    FrameworkCriterionSignalImportance importance,
    String evidenceHint
) {
    public FrameworkCriterionSignal {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("The signal code must not be blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("The signal description must not be blank");
        }
        if (importance == null) {
            throw new IllegalArgumentException("The signal importance must not be blank");
        }
        if (evidenceHint != null && evidenceHint.isBlank()) {
            throw new IllegalArgumentException("The evidence signal should be left empty or must not consist only of whitespace (if present)");
        }
    }
}
