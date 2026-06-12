package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.CreateSystemRubricCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemRubricRequest;

import java.util.List;
import java.util.stream.Collectors;

public class CreateSystemRubricCommandMapper {
    public static CreateSystemRubricCommand fromRequest(CreateSystemRubricRequest request) {

        List<CreateSystemRubricCommand.RubricVersionItemCommand> versionCommands = request.versions()
                .stream()
                .map(v -> new CreateSystemRubricCommand.RubricVersionItemCommand(
                        v.version(),
                        v.scoringScaleMin(),
                        v.scoringScaleMax(),
                        v.totalScoreMethod(),
                        DateMapper.toOffsetDateTime(v.effectiveFrom()),
                        DateMapper.toOffsetDateTime(v.effectiveTo())
                ))
                .collect(Collectors.toList());

        return new CreateSystemRubricCommand(
                request.code(),
                request.name(),
                request.description(),
                request.languageId(),
                request.frameworkId(),
                versionCommands
        );
    }
}