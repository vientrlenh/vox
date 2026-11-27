package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.dto.QuestionAssetDto;

/**
 * ONE QUESTION to grade: the speaking turn to listen to, the score currently in effect for reference,
 * and the evidence of the AI grade so the teacher grades on the same data the AI used.
 *
 * <p>{@code partLabel} is the section title, so many questions share one label --
 * {@code sectionId} + {@code orderInSection} is what tells them apart.
 *
 * <p>The {@code ai*} fields come from the AI grade, NOT the grade in effect: after manual grading the
 * effective grade is the HUMAN one (without the AI's turn/signals/rationale), but the appeal round must
 * still be able to read the original evidence. A submission without an AI grade leaves null/false/empty.
 */
public record GradingTaskItemInfo(
    UUID paperItemId,
    UUID responseId,
    String partLabel,
    UUID sectionId,
    int orderInSection,
    /**
     * The prompt of the question, taken straight from {@code questions.question_text} through
     * {@code assessment_paper_items.question_id} -- the same source as the Result screen
     * ({@code ViewAssessmentSessionResultUseCase#itemResponses}).
     *
     * <p>Previously the grading screen did NOT have this field: the teacher only saw "Part 1, question 2" and had to
     * guess the prompt through the {@code promptText} of the spoken turn. But that text is the line the AI reads when entering the turn
     * ("You have 5 seconds to get ready...") and not the prompt, so grading without knowing what the question
     * is -- one cannot judge whether the answer is on target.
     *
     * <p>{@code null} when the paper item does not point to any question or the question has been deleted.
     */
    String questionText,
    /**
     * The resource that comes with the question (image / audio / video / passage), {@code null} when the question has none.
     *
     * <p>Without this field, a teacher grading a describe-the-picture question sees only the prompt
     * "Describe the picture." and the candidate's transcript, WITHOUT the picture -- no way to tell
     * whether the candidate described it correctly or made it up. For a listening question it is worse:
     * if the teacher cannot hear the passage the candidate heard, the content part cannot be graded.
     *
     * <p>It matters even more since the AI only knows the asset through the {@code transcript}/{@code description}
     * typed by the author: a human is the last line of defense against exactly that kind of mistake, and
     * that line is blindfolded. This is also the only place that can detect a broken asset that does not
     * play on the candidate's machine.
     */
    QuestionAssetDto asset,
    BigDecimal currentItemScore,
    String currentFeedbackSummary,
    List<GradingCriterionScoreInfo> currentScores,
    List<GradingTurnInfo> turns,
    List<GradingCriterionScoreInfo> aiScores,
    BigDecimal aiOverallConfidence,
    String aiFeedbackSummary,
    boolean aiRequiresHumanReview,
    String aiReviewReasonCode,
    boolean aiMarkedInvalid,
    boolean aiRequiresRetake,
    String aiSignals,
    String aiValidity
) {
}
