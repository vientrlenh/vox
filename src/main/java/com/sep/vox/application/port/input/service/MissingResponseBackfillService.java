package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseTurnRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;


@Service
@RequiredArgsConstructor
@Slf4j
public class MissingResponseBackfillService {

    /**
     * Label written to {@code termination_reason} -- it tells this case apart from a question the candidate did attempt but that was cut
     * off midway ({@code redirect_offtopic}, {@code timeout}...). This question never started.
     */
    public static final String NO_RECORDING = "no_recording";

    /**
     * {@code engine_type} borrows AI_SINGLE because the column has a CHECK that only accepts AI_SINGLE/AI_ENSEMBLE/HUMAN,
     * and {@code ddl-auto} cannot alter a CHECK, so a new value would need its own migration.
     * The real value is in {@code graded_by_model} below.
     *
     * <p>Writing HUMAN would be worse: the detail screen branches on {@code engineType == HUMAN}, so this question
     * would show as "graded by a teacher" although nobody graded it.
     */
    private static final String GRADED_BY = "SYSTEM_NO_RECORDING";

    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository;
    private final AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;

    /**
     * Does this answer really have NO content at all.
     *
     * <p>Looks at the transcript of EACH TURN too, not only the response's total cell: some write paths only
     * fill in the transcript at turn level, so looking at the response alone would wrongly conclude it is empty.
     *
     * <p>The definition lives here instead of being written separately at each call site, because exactly that
     * happened before: the submit path had this guard, while {@code UpholdResultUseCase} did not -- and an
     * answer a student spoke in 2 turns (41 seconds + 36 seconds, with both audio and transcript) was recorded as 0 points with the
     * sentence "The candidate gave no answer to this question" (measured 2026-08-17, session
     * 01a0101d). One predicate, one place to define it.
     */
    public boolean isSilentAnswer(AssessmentItemResponse response) {
        if (response.getTranscript() != null && !response.getTranscript().isBlank()) {
            return false;
        }
        return assessmentItemResponseTurnRepository.findByAssessmentItemResponseId(response.getId()).stream()
            .noneMatch(turn -> turn.getTranscript() != null && !turn.getTranscript().isBlank());
    }

    /**
     * Records a 0-point grade for a response that ALREADY EXISTS but where the candidate said nothing.
     *
     * <p>Different from {@link #backfill}: there the question was never presented so no row exists; here the
     * candidate WAS asked, the AI asked again up to the cap and then cut the question off, yet the transcript is still empty.
     *
     * <p>Why not let the LLM grade it: it receives the full question but an empty answer, and will still return
     * a score -- inferred from thin air, indistinguishable from a real score. When nothing was said,
     * 0 is the only correct answer, and writing it directly here is both certain and saves a model
     * call.
     *
     * <p>Checks {@link #isSilentAnswer} itself instead of trusting the caller: this function writes 0 points directly
     * with the sentence "the candidate gave no answer", i.e. it makes a statement about the student. That statement
     * must be decided by the data itself, not by whether the caller remembers or forgets to check.
     *
     * @return true if recorded (i.e. this response really is empty); false when the question has content and
     *         must not be given 0
     */
    public boolean recordSilentAnswer(UUID responseId, UUID paperItemId) {
        AssessmentItemResponse response = assessmentItemResponseRepository.findById(responseId).orElse(null);
        if (response != null && !isSilentAnswer(response)) {
            log.warn(
                "Refusing to record 0 points for question {}: this question HAS answer content and must be handled by a grader.",
                responseId
            );
            return false;
        }
        assessmentItemEvaluationRepository.save(AssessmentItemEvaluation.zeroScoredWithoutRecording(
                responseId,
                paperItemId,
                GRADED_BY,
                NO_RECORDING,
                "The candidate gave no answer to this question.",
                Instant.now()));
        log.info("Question {} has no answer content -- recording 0 points, not calling the LLM.", responseId);
        return true;
    }

    /**
     * @return the number of questions backfilled; 0 when the candidate answered the whole paper (the most common path)
     */
    public int backfill(UUID sessionId, UUID paperId) {
        if (paperId == null) {
            return 0;
        }
        Set<UUID> answeredPaperItemIds = assessmentItemResponseRepository.findBySessionId(sessionId).stream()
            .map(response -> response.getPaperItemId())
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());

        Instant now = Instant.now();
        int created = 0;
        for (AssessmentPaperItem paperItem : assessmentPaperItemRepository.findByPaperId(paperId)) {
            if (answeredPaperItemIds.contains(paperItem.getId())) {
                continue;
            }
            backfillMissingItem(sessionId, paperItem, now);
            created++;
        }
        if (created > 0) {
            log.info("Session {}: filling {} questions with no recording (0 points, awaiting teacher review).",
                sessionId, created);
        }
        return created;
    }

    private void backfillMissingItem(UUID sessionId, AssessmentPaperItem paperItem, Instant now) {
        // The id is left null on purpose: assessment_item_responses.id keeps the answerId the AI service sends
        // for recorded answers, and generates a version 7 id for a row that has none, like this one. The evaluation
        // below must use the id of the SAVED response, never one chosen before the save.
        AssessmentItemResponse response = assessmentItemResponseRepository.save(AssessmentItemResponse.withoutRecording(
                sessionId, paperItem.getId(), NO_RECORDING, now));
        saveZeroScoredEvaluation(response, paperItem, now);
    }

    /**
     * requiresHumanReview = FALSE. With no recording a score of 0 is correct and nobody needs to
     * review it again.
     *
     * <p>Setting it to true was tried, with the argument "the student may appeal that the machine failed to
     * record", and dropped: the data CANNOT tell "the student gave no answer" from "the machine broke" -- both
     * are just a missing response row. Raising the flag would assert something the system does not
     * know, and flagging every blank question would flood the review queue with results where 0 is
     * already right, making the flag meaningless.
     *
     * <p>The appeal path already exists and is in the right place: when the student files an appeal the
     * APPEAL round opens, and thanks to this backfill the teacher HAS a question to regrade. That is
     * the value of the backfill -- it makes the question EXIST, not make someone go and review it.
     */
    private void saveZeroScoredEvaluation(AssessmentItemResponse response, AssessmentPaperItem paperItem, Instant now) {
        assessmentItemEvaluationRepository.save(AssessmentItemEvaluation.zeroScoredWithoutRecording(
                response.getId(),
                paperItem.getId(),
                GRADED_BY,
                NO_RECORDING,
                "No recording was captured for this question.",
                now));
    }
}
