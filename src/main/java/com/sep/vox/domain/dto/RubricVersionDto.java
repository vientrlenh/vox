package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;

public record RubricVersionDto(
        UUID id,
        UUID rubricId,
        int version,
        String code,
        String name,
        String description,
        RubricStatus status,
        Instant effectiveFrom,
        Instant effectiveTo,
        BigDecimal scoringScaleMin,
        BigDecimal scoringScaleMax,
        RubricTotalScoreMethod totalScoreMethod,
        Instant createdAt,
        /**
         * The template version this one was copied from, or null if the owner wrote it themselves.
         *
         * <p>With it the version page can say "Copied from the system's template criteria set" instead
         * of leaving the copy looking exactly like a self-written one, and later it can be compared
         * when the system publishes a version newer than the one the school is using.
         */
        UUID sourceRubricVersionId
) {}
