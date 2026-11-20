package com.sep.vox.interfaces.graphql.mapper;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.UpdateSchoolRubricVersionCommand;
import com.sep.vox.application.port.input.command.UpdateSystemRubricVersionCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateRubricVersionInput;

import java.util.UUID;

public class UpdateRubricVersionGraphQLMapper {
    public static UpdateSystemRubricVersionCommand fromSystemInput(UUID versionId, UpdateRubricVersionInput input) {
        return new UpdateSystemRubricVersionCommand(
                versionId,
                input.name(),
                input.description(),
                DateMapper.toInstant(input.effectiveFrom()),
                DateMapper.toInstant(input.effectiveTo()),
                input.scoringScaleMin(),
                input.scoringScaleMax(),
                input.totalScoreMethod()
        );
    }

    public static UpdateSchoolRubricVersionCommand fromSchoolInput(UUID schoolId, UUID versionId, UpdateRubricVersionInput input) {
        return new UpdateSchoolRubricVersionCommand(
                schoolId,
                versionId,
                input.name(),
                input.description(),
                DateMapper.toInstant(input.effectiveFrom()),
                DateMapper.toInstant(input.effectiveTo()),
                input.scoringScaleMin(),
                input.scoringScaleMax(),
                input.totalScoreMethod()
        );
    }
}
