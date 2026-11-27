package com.sep.vox.application.response.input.assessmentsession;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.dto.QuestionAssetDto;

/**
 * @param questionText the prompt of this question, taken straight from {@code questions.question_text}.
 *                     Before, this field did not exist so the student result page had to derive the question
 *                     content from {@code turns[].promptText} -- which both required opening the card before loading
 *                     (when closed it showed "No question content." even though the data was intact),
 *                     and picked up the AI's lead-in ("You have 10 seconds to prepare...") instead of
 *                     the prompt. Null when the paper item points to no question, or the question was
 *                     removed from the bank.
 */
public record AssessmentCandidateResultItemResponse(
    UUID paperItemId,
    UUID responseId,
    UUID sectionId,
    String questionText,
    /**
     * The resource that comes with the question, {@code null} when the question has none.
     *
     * <p>The same reason as the grading screen: reviewing a submission without seeing the image / being able to replay the
     * listening passage makes it impossible to check the answer against the prompt. Shown to BOTH teachers and students --
     * this page already returns {@code questionText}, so the asset reveals nothing extra, and the
     * candidate has just looked at that very image in the exam room.
     */
    QuestionAssetDto asset,
    BigDecimal itemScore,
    BigDecimal weightedScore
) {
}
