package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSchoolRubricCriterionBandsCommand;
import com.sep.vox.application.port.input.command.CreateSystemRubricCriterionBandsCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolRubricCriterionBandsRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemRubricCriterionBandsRequest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreateRubricCriterionBandsCommandMapper {


    public static CreateSystemRubricCriterionBandsCommand fromSystemRequest(
            UUID versionId, UUID criterionId, CreateSystemRubricCriterionBandsRequest request) {

        List<CreateSystemRubricCriterionBandsCommand.CriterionBandItemCommand> bandCommands = request.bands()
                .stream()
                .map(b -> new CreateSystemRubricCriterionBandsCommand.CriterionBandItemCommand(
                        b.code(),
                        b.scoreMin(),
                        b.scoreMax()
                ))
                .collect(Collectors.toList());

        return new CreateSystemRubricCriterionBandsCommand(versionId, criterionId, bandCommands);
    }

    public static CreateSchoolRubricCriterionBandsCommand fromSchoolRequest(
            UUID schoolId, UUID versionId, UUID criterionId, CreateSchoolRubricCriterionBandsRequest request) {

        List<CreateSchoolRubricCriterionBandsCommand.CriterionBandItemCommand> bandCommands = request.bands()
                .stream()
                .map(b -> new CreateSchoolRubricCriterionBandsCommand.CriterionBandItemCommand(
                        b.code(),
                        b.scoreMin(),
                        b.scoreMax()
                ))
                .collect(Collectors.toList());

        return new CreateSchoolRubricCriterionBandsCommand(schoolId, versionId, criterionId, bandCommands);
    }
}
