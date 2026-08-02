package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface WeaknessScoreObservationRow {

    UUID getStudentId();

    UUID getFrameworkCriterionId();

    String getCriterionCode();

    BigDecimal getFinalScore();

    BigDecimal getMinScore();

    BigDecimal getMaxScore();

    OffsetDateTime getEvaluatedAt();

    String getSourceType();

    UUID getEvaluationId();

    UUID getSchoolClassId();

    UUID getSchoolGradeId();
}
