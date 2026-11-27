package com.sep.vox.application.port.input.command.assessment;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * @param paperId the paper that receives the new section. May be left empty only when the assessment has exactly one paper.
 */
public record CreateClassTestSectionCommand(
    UUID assessmentId,
    UUID paperId,
    String title,
    String instruction,
    BigDecimal weight,
    List<ClassTestQuestionCommand> questions
) {
}
