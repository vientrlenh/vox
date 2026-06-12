package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSystemRubricCriteriaCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemRubricCriteriaRequest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreateSystemRubricCriteriaCommandMapper {
    public static CreateSystemRubricCriteriaCommand fromRequest(UUID versionId, CreateSystemRubricCriteriaRequest request) {

        List<CreateSystemRubricCriteriaCommand.CriterionItemCommand> criteriaCommands = request.criteria()
                .stream()
                .map(c -> new CreateSystemRubricCriteriaCommand.CriterionItemCommand(
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

        return new CreateSystemRubricCriteriaCommand(
                versionId,
                criteriaCommands
        );
    }
}