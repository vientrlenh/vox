package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.ClassTestSectionCommand;
import com.sep.vox.application.port.input.command.CreateAssessmentPaperCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateAssessmentPaperRequest;

public final class CreateAssessmentPaperCommandMapper {

    private CreateAssessmentPaperCommandMapper() {
    }

    public static CreateAssessmentPaperCommand fromRequest(UUID assessmentId, CreateAssessmentPaperRequest request) {
        if (request == null) {
            return new CreateAssessmentPaperCommand(assessmentId, null, null, null);
        }
        return new CreateAssessmentPaperCommand(
            assessmentId,
            request.source(),
            request.copyFromPaperId(),
            request.sections() == null ? null : request.sections().stream()
                .map(section -> new ClassTestSectionCommand(
                    section.title(),
                    section.instruction(),
                    section.weight(),
                    ClassTestQuestionCommandMapper.fromRequests(section.questions())
                ))
                .toList()
        );
    }
}
