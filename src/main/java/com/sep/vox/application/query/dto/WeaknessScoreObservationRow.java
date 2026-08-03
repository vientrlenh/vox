package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface WeaknessScoreObservationRow {

    UUID getStudentId();

    UUID getFrameworkCriterionId();

    String getCriterionCode();

    BigDecimal getFinalScore();

    BigDecimal getMinScore();

    BigDecimal getMaxScore();

    // Instant: xem chu thich trong SessionRowInfo. Domain model cung dung Instant nen map thang,
    // khong con buoc doi kieu nao.
    Instant getEvaluatedAt();

    String getSourceType();

    UUID getEvaluationId();

    UUID getSchoolClassId();

    UUID getSchoolGradeId();
}
