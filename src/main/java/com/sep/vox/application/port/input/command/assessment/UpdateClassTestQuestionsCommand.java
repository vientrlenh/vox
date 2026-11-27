package com.sep.vox.application.port.input.command.assessment;

import java.util.List;
import java.util.UUID;

/**
 * @param paperId the paper whose entire content is replaced. May be left empty only when the assessment has exactly one paper.
 */
public record UpdateClassTestQuestionsCommand(
    UUID assessmentId,
    UUID paperId,
    List<ClassTestSectionCommand> sections
) {
}
