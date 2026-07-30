package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.*;

import java.util.UUID;

public class DeleteSystemRubricCommandMapper {
    // Cho rubric
    public static DeleteSystemRubricCommand fromRequest(UUID rubricId) {
        return new DeleteSystemRubricCommand(rubricId);
    }

    // 1. Cho Version
    public static DeleteSystemRubricVersionCommand versionFromRequest(UUID versionId) {
        return new DeleteSystemRubricVersionCommand(versionId);
    }

    // 2. Cho Criterion
    public static DeleteSystemRubricCriterionCommand criterionFromRequest(UUID versionId, UUID criterionId) {
        return new DeleteSystemRubricCriterionCommand(versionId, criterionId);
    }

    // 3. Cho Result Band
    public static DeleteSystemRubricResultBandCommand resultBandFromRequest(UUID versionId, UUID resultBandId) {
        return new DeleteSystemRubricResultBandCommand(versionId, resultBandId);
    }
}