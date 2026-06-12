package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSchoolRubricCriteriaCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolRubricCriteriaRequest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreateSchoolRubricCriteriaCommandMapper {
    public static CreateSchoolRubricCriteriaCommand fromRequest(UUID schoolId, UUID versionId, CreateSchoolRubricCriteriaRequest request) {

        // Map từng Request Item sang Command Item
        List<CreateSchoolRubricCriteriaCommand.CriterionItemCommand> criteriaCommands = request.criteria()
                .stream()
                .map(c -> new CreateSchoolRubricCriteriaCommand.CriterionItemCommand(
                        c.frameworkCriterionId(),
                        c.code(),
                        c.name(),
                        c.description(),
                        c.weight(),
                        c.minScore(),
                        c.maxScore(),
                        c.order(),
                        c.isRequired()
                ))
                .collect(Collectors.toList());

        return new CreateSchoolRubricCriteriaCommand(
                schoolId,
                versionId,
                criteriaCommands
        );
    }
}