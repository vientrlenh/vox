package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.DeleteSchoolRubricCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolRubricCriterionCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolRubricResultBandCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolRubricVersionCommand;

import java.util.UUID;

public class DeleteSchoolRubricCommandMapper {

    // 0. Cho Rubric gốc
    public static DeleteSchoolRubricCommand fromRequest(UUID schoolId, UUID rubricId) {
        return new DeleteSchoolRubricCommand(schoolId, rubricId);
    }

    // 1. Cho Version
    public static DeleteSchoolRubricVersionCommand versionFromRequest(UUID schoolId, UUID versionId) {
        return new DeleteSchoolRubricVersionCommand(schoolId, versionId);
    }

    // 2. Cho Criterion
    public static DeleteSchoolRubricCriterionCommand criterionFromRequest(UUID schoolId, UUID versionId, UUID criterionId) {
        return new DeleteSchoolRubricCriterionCommand(schoolId, versionId, criterionId);
    }

    // 3. Cho Result Band
    public static DeleteSchoolRubricResultBandCommand resultBandFromRequest(UUID schoolId, UUID versionId, UUID resultBandId) {
        return new DeleteSchoolRubricResultBandCommand(schoolId, versionId, resultBandId);
    }
}
