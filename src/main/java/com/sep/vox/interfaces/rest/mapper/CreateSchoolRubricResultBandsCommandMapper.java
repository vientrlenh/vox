package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSchoolRubricResultBandsCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolRubricResultBandsRequest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreateSchoolRubricResultBandsCommandMapper {
    public static CreateSchoolRubricResultBandsCommand fromRequest(UUID schoolId, UUID versionId, CreateSchoolRubricResultBandsRequest request) {

        List<CreateSchoolRubricResultBandsCommand.ResultBandItemCommand> bandCommands = request.resultBands()
                .stream()
                .map(b -> new CreateSchoolRubricResultBandsCommand.ResultBandItemCommand(
                        b.frameworkResultBandId(),
                        b.code(),
                        b.name(),
                        b.description(),
                        b.mappedScoreMin(),
                        b.mappedScoreMax(),
                        b.order(),
                        b.isPassing()
                ))
                .collect(Collectors.toList());

        return new CreateSchoolRubricResultBandsCommand(schoolId, versionId, bandCommands);
    }
}