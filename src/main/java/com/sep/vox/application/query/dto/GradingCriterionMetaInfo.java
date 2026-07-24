package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Tiêu chí rubric kèm dải điểm thật. FE dựng ô nhập từ min/max ở đây thay vì
 * hardcode 0-9 — mỗi rubric một thang khác nhau.
 */
public record GradingCriterionMetaInfo(
    UUID id,
    String code,
    String label,
    String description,
    BigDecimal minScore,
    BigDecimal maxScore,
    BigDecimal weight,
    boolean required
) {
}
