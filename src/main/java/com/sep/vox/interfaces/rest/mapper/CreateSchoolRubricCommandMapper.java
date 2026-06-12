package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.CreateSchoolRubricCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolRubricRequest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreateSchoolRubricCommandMapper {
    public static CreateSchoolRubricCommand fromRequest(UUID schoolId, CreateSchoolRubricRequest request) {

        // Map List<Request> sang List<Command>
        List<CreateSchoolRubricCommand.RubricVersionItemCommand> versionCommands = request.versions()
                .stream()
                .map(v -> new CreateSchoolRubricCommand.RubricVersionItemCommand(
                        v.version(),
                        v.scoringScaleMin(),
                        v.scoringScaleMax(),
                        v.totalScoreMethod(),
                        DateMapper.toOffsetDateTime(v.effectiveFrom()),
                        DateMapper.toOffsetDateTime(v.effectiveTo())
                ))
                .collect(Collectors.toList());

        // Khởi tạo Command cha chứa danh sách version
        return new CreateSchoolRubricCommand(
                schoolId,
                request.code(),
                request.name(),
                request.description(),
                request.languageId(),
                request.frameworkId(),
                versionCommands
        );
    }
}