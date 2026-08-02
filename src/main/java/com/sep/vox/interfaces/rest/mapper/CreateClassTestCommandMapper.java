package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.ClassTestSectionCommand;
import com.sep.vox.application.port.input.command.CreateClassTestCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateClassTestRequest;

public final class CreateClassTestCommandMapper {

    private CreateClassTestCommandMapper() {
    }

    public static CreateClassTestCommand fromRequest(CreateClassTestRequest request) {
        return new CreateClassTestCommand(
            request.schoolClassId(),
            request.name(),
            request.description(),
            request.openAt(),
            request.closeAt(),
            request.assessmentPolicyId(),
            request.sections() == null ? null : request.sections().stream()
                .map(section -> new ClassTestSectionCommand(
                    section.title(),
                    section.instruction(),
                    section.weight(),
                    ClassTestQuestionCommandMapper.fromRequests(section.questions())
                ))
                .toList(),
            request.existingBlueprintId(),
            request.existingBlueprintVersionId(),
            request.maxAttempt(),
            request.examTimeDurationSecond(),
            request.resultDecisionMethod()
        );
    }
}
