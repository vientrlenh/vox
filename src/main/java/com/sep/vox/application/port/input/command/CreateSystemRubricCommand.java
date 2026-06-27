package com.sep.vox.application.port.input.command;

import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateSystemRubricCommand(
        String code,
        String name,
        String description,
        UUID languageId,
        UUID frameworkId
) {
}