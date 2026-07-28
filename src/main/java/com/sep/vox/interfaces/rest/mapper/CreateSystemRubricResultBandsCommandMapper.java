package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSystemRubricResultBandsCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemRubricResultBandsRequest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreateSystemRubricResultBandsCommandMapper {
    public static CreateSystemRubricResultBandsCommand fromRequest(UUID versionId, CreateSystemRubricResultBandsRequest request) {

        List<CreateSystemRubricResultBandsCommand.ResultBandItemCommand> bandCommands = request.resultBands()
                .stream()
                .map(b -> new CreateSystemRubricResultBandsCommand.ResultBandItemCommand(
                        b.code(),
                        b.name(),
                        b.description(),
                        b.mappedScoreMin(),
                        b.mappedScoreMax(),
                        b.order()
                ))
                .collect(Collectors.toList());

        return new CreateSystemRubricResultBandsCommand(versionId, bandCommands);
    }
}
