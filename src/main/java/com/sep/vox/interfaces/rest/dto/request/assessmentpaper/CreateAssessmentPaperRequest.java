package com.sep.vox.interfaces.rest.dto.request.assessmentpaper;

import com.sep.vox.application.port.input.command.assessment.ClassTestSectionCommand;
import com.sep.vox.application.port.input.command.assessmentpaper.CreateAssessmentPaperCommand;
import com.sep.vox.interfaces.rest.dto.request.assessment.ClassTestQuestionRequest;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

/**
 * @param source   {@code blueprint} (default) | {@code copy} | {@code questions}.
 * @param sections only used when {@code source = questions} -- authoring questions directly, for a class
 *                 test only.
 */
public record CreateAssessmentPaperRequest(
    String source,
    UUID copyFromPaperId,

    @Valid
    List<ClassTestSectionRequest> sections
) {

    public static CreateAssessmentPaperCommand toCommand(UUID assessmentId, CreateAssessmentPaperRequest request) {
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
                    ClassTestQuestionRequest.toCommands(section.questions())
                ))
                .toList()
        );
    }
}
