package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSchoolRubricCriteriaCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolRubricCriteriaRequest;

import java.util.List;
import java.util.UUID;

public class CreateSchoolRubricCriteriaCommandMapper {
    public static CreateSchoolRubricCriteriaCommand fromRequest(UUID schoolId, UUID versionId, CreateSchoolRubricCriteriaRequest request) {

        List<CreateSchoolRubricCriteriaCommand.CriterionItemCommand> criteriaCommands = request.criteria().stream()
                .map(c -> {
                    // Map danh sách Example
                    List<CreateSchoolRubricCriteriaCommand.CriterionExampleCommand> exampleCommands = null;
                    if (c.examples() != null) {
                        exampleCommands = c.examples().stream()
                                .map(ex -> new CreateSchoolRubricCriteriaCommand.CriterionExampleCommand(
                                        ex.transcript(), ex.explanation(), ex.expectedScore()
                                )).toList();
                    }

                    return new CreateSchoolRubricCriteriaCommand.CriterionItemCommand(
                            c.frameworkCriterionId(),
                            c.code(),
                            c.name(),
                            c.description(),
                            exampleCommands, // Truyền Examples vào đây
                            c.weight(),
                            c.minScore(),
                            c.maxScore(),
                            c.order(),
                            c.isRequired()
                    );
                }).toList();

        return new CreateSchoolRubricCriteriaCommand(schoolId, versionId, criteriaCommands);
    }
}