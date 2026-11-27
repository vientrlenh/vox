package com.sep.vox.application.port.input.command.assessmentpaper;

import com.sep.vox.application.port.input.command.assessment.ClassTestSectionCommand;
import java.util.List;
import java.util.UUID;

/**
 * @param source     {@code blueprint} | {@code copy} | {@code questions}.
 * @param sections   only used when {@code source = questions} (composing questions directly -- for
 *                   class tests only).
 */
public record CreateAssessmentPaperCommand(
    UUID assessmentId,
    String source,
    UUID copyFromPaperId,
    List<ClassTestSectionCommand> sections
) {
}
