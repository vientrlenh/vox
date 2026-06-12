package com.sep.vox.domain.valueobject.framework;

public record FrameworkCriterionSignal(
    String code,
    String description,
    FrameworkCriterionSignalImportance importance,
    String evidenceHint
) {
    public FrameworkCriterionSignal {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code của dấu hiệu không được để trống");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Mô tả của dấu hiệu không được để trống");
        }
        if (importance == null) {
            throw new IllegalArgumentException("Độ quan trọng của dấu hiệu không được để trống");
        }
        if (evidenceHint != null && evidenceHint.isBlank()) {
            throw new IllegalArgumentException("Dấu hiệu của chứng cứ nên bỏ trống hoặc không hoàn toàn chứa khoảng trắng (nếu có)");
        }
    }
}
