package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSystemRubricCriteriaCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemRubricCriteriaRequest;

import java.util.List;
import java.util.UUID;

public class CreateSystemRubricCriteriaCommandMapper {
    public static CreateSystemRubricCriteriaCommand fromRequest(UUID versionId, CreateSystemRubricCriteriaRequest request) {

        List<CreateSystemRubricCriteriaCommand.CriterionItemCommand> criteriaCommands = request.criteria().stream()
                .map(c -> {
                    // Map danh sách Example
                    List<CreateSystemRubricCriteriaCommand.CriterionExampleCommand> exampleCommands = null;
                    if (c.examples() != null) {
                        exampleCommands = c.examples().stream()
                                .map(ex -> new CreateSystemRubricCriteriaCommand.CriterionExampleCommand(
                                        ex.transcript(), ex.explanation(), ex.expectedScore()
                                )).toList();
                    }

                    return new CreateSystemRubricCriteriaCommand.CriterionItemCommand(
                            c.frameworkCriterionId(),
                            c.code(),
                            c.name(),
                            c.description(),
                            exampleCommands, // BỔ SUNG TRUYỀN VÀO ĐÂY
                            c.weight(),
                            c.minScore(),
                            c.maxScore(),
                            c.order(),
                            c.isRequired()
                    );
                }).toList();

        return new CreateSystemRubricCriteriaCommand(versionId, criteriaCommands);
    }
}