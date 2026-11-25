package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.assessmentgrading.GradingDecisionCommand;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentgrading.GradingActionResponse;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundPolicy;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.application.port.input.service.MissingResponseBackfillService;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.UpsertAssessmentCandidateResultUseCase;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * Giáo viên xác nhận điểm đang có là đúng — không nhập điểm mới.
 *
 * <p>Đây là hành động gộp của bản rework: "admin release bài chờ chấm" và "hậu kiểm
 * rồi giữ nguyên" hoá ra là cùng một quyết định (điểm hiện có đúng), chỉ khác vòng.
 * Vòng {@code INITIAL} thì bài chuyển sang RELEASED; vòng {@code SPOT_CHECK} thì bài
 * vốn đã RELEASED nên không đổi gì và <em>không</em> gửi mail.
 *
 * <p>Không tính lại điểm: không có item nào thay đổi, nên tổng vẫn là hàm của đúng
 * các item cũ. Bất biến {@code total = f(items)} được giữ mà không phải chạm gì.
 */
@Service
@RequiredArgsConstructor
public class UpholdResultUseCase implements IUseCase<GradingDecisionCommand, GradingActionResponse> {

    private final GradingActionSupport gradingActionSupport;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private final MissingResponseBackfillService missingResponseBackfillService;
    private final UpsertAssessmentCandidateResultUseCase upsertAssessmentCandidateResultUseCase;

    @Override
    @Transactional
    public GradingActionResponse execute(GradingDecisionCommand command) {
        GradingActionSupport.PreparedAction prepared = gradingActionSupport.prepare(
            command.assignmentId(), GradingOutcome.UPHELD, command.reason());

        AssessmentSession session = prepared.context().session();
        clearSuspicionFlag(prepared, session);

        AssessmentCandidateResult result = prepared.context().candidateResult();
        AssessmentCandidateResultStatus targetStatus = GradingRoundPolicy.resultStatusAfter(
            prepared.roundType(), GradingOutcome.UPHELD);
        AssessmentCandidateResult finalResult = fillAndRecalculateIfPublishing(result, session, targetStatus);

        gradingActionSupport.finish(prepared, finalResult);

        return new GradingActionResponse(
            command.assignmentId(),
            finalResult.getId(),
            GradingOutcome.UPHELD.name(),
            finalResult.getStatus() == null ? null : finalResult.getStatus().name(),
            finalResult.getTotalScore(),
            null
        );
    }

    /**
     * Clears the suspicion flag: the teacher has looked at it and concluded there is no violation. The
     * flagReason is kept so it can still be looked up why the attempt was once flagged.
     *
     * <p>In REMEDIATION it is NOT cleared: keeping the attempt as it is in that round means confirming the
     * violation, and clearing the flag now would say the opposite of the decision just recorded.
     */
    private void clearSuspicionFlag(GradingActionSupport.PreparedAction prepared, AssessmentSession session) {
        if (prepared.roundType() != GradingRoundType.REMEDIATION && session.isFlagged()) {
            session.setFlagged(false);
            assessmentSessionRepository.save(session);
        }
    }

    /**
     * The "must be fully graded" guard below applies ONLY to the rounds where keeping the score means PUBLISHING
     * (INITIAL, APPEAL -- see the matrix in GradingRoundPolicy). That is where a missing score leaks out
     * to the student, so it is also the only place worth blocking.
     *
     * <p>The REMEDIATION round is outside it: the attempt there is INVALID and stays INVALID (targetStatus
     * null), and keeping it means CONFIRMING THE VIOLATION, not keeping a score. An attempt that was
     * forcibly ended during the session has by design no evaluation
     * (SubmitAssessmentSessionUseCase exits early in the blockedAt branch), so this guard would hit EXACTLY
     * the attempts the REMEDIATION round was created to handle: the teacher cannot grade it (there is
     * nothing to grade), cannot keep it as it is, and the assignment becomes a dead end with no way to close it.
     * The post-check (SPOT_CHECK) is also outside it because the attempt is already RELEASED -- blocking there recovers
     * nothing, it only locks out the person who is reviewing the attempt.
     *
     * <p>The score is recalculated only when something was really just filled in -- an attempt that is already
     * fully graded keeps the old path and the existing score is not touched.
     */
    private AssessmentCandidateResult fillAndRecalculateIfPublishing(
            AssessmentCandidateResult result, AssessmentSession session,
            AssessmentCandidateResultStatus targetStatus) {
        boolean publishesResult = targetStatus == AssessmentCandidateResultStatus.RELEASED;
        boolean filledAny = publishesResult && fillSilentAnswersOrRefuse(result.getSessionId(), session);
        if (!filledAny) {
            return result;
        }
        return upsertAssessmentCandidateResultUseCase.execute(
            result.getSessionId(),
            targetStatus == null ? result.getStatus() : targetStatus);
    }

    /**
     * Fills in the questions that have no grade yet before the assessment is released, or refuses if they
     * cannot be filled in. ONLY call it in the round where keeping the score = releasing.
     *
     * <p>"Keep the score" on a question that has NO grade at all means keeping an empty number.
     * A question the candidate said nothing on gets a plain 0 and is then finalised; a question the
     * candidate did answer is blocked.
     *
     * <p>Measured on 2026-08-17 on session 01a00e64: a 2-question assessment, 0 evaluations, pressing
     * Uphold in the INITIAL round pushed the result to RELEASED with {@code total_score = NULL} -- an
     * assessment that is RELEASED but has no score. The result page then breaks outright because the
     * calculator meets a question without an evaluation.
     *
     * <p>Reuses {@code recordSilentAnswer} (0 points, no LLM call) instead of adding a new rule: that is
     * already how the system handles "no content to grade" on the submit path.
     *
     * @return true if a grade of 0 was just written -- meaning the total score must be recalculated
     * @throws IllegalArgumentException when a question the candidate DID answer still has no grade from anyone
     */
    private boolean fillSilentAnswersOrRefuse(UUID sessionId, AssessmentSession session) {
        backfillMissingResponsesOfClosedSession(sessionId, session);

        List<AssessmentItemResponse> responses = assessmentItemResponseRepository.findBySessionId(sessionId);
        if (responses.isEmpty()) {
            return false;
        }
        List<AssessmentItemResponse> ungraded = ungradedResponses(responses);
        requireNoAnsweredButUngraded(ungraded);

        boolean filledAny = false;
        for (AssessmentItemResponse response : ungraded) {
            filledAny |= missingResponseBackfillService.recordSilentAnswer(
                response.getId(), response.getPaperItemId());
        }
        return filledAny;
    }

    /**
     * A question the candidate NEVER attempted has no response row, so the loop in the caller does not see
     * it. If it is not filled in, the total score is averaged over only the questions that WERE attempted -- an attempt of 2 questions that skipped 1
     * question gets 8 instead of 4. It has to be filled in BEFORE the early-exit branch there, because an attempt with no response
     * at all is the one that needs filling in the most.
     *
     * <p>But only fill in when the session has really closed. GradingActionSupport.prepare only checks the status of the
     * ASSIGNMENT and the RESULT, not the status of the SESSION -- so a teacher can click keep-the-score
     * while the candidate is still in the middle of the attempt. That path is real: ClearInvalidResultUseCase
     * both clears blockedAt so the candidate can resume the attempt and opens the INITIAL round for the teacher to finalize.
     *
     * <p>RESUMABLE = {IN_PROGRESS, INTERRUPTED} means exactly "the candidate can still come back and answer",
     * so that is the predicate to ask. Filling in early would repeat the two-responses-for-one-question bug that
     * ForceEndAssessmentSessionUseCase has just had to remove.
     */
    private void backfillMissingResponsesOfClosedSession(UUID sessionId, AssessmentSession session) {
        if (!AssessmentSessionStatus.RESUMABLE.contains(session.getStatus())) {
            missingResponseBackfillService.backfill(sessionId, session.getPaperId());
        }
    }

    private List<AssessmentItemResponse> ungradedResponses(List<AssessmentItemResponse> responses) {
        Set<UUID> evaluatedResponseIds = assessmentItemEvaluationRepository
            .findByResponseIdIn(responses.stream().map(response -> response.getId()).toList()).stream()
            .map(evaluation -> evaluation.getResponseId())
            .collect(Collectors.toSet());
        return responses.stream()
            .filter(response -> !evaluatedResponseIds.contains(response.getId()))
            .toList();
    }

    /**
     * Only a question that is TRULY empty may be given 0. A question the candidate did speak on but nobody has
     * graded cannot be "kept as is" -- there is no score there to keep.
     *
     * <p>The first version of this branch gave 0 to every question without an evaluation, without asking
     * whether the question had content. Measured 2026-08-17 on session 01a0101d: a question answered in 2 turns
     * (41 s + 36 s, with both audio and transcript) was written as 0 points with "the candidate gave no answer
     * to this question" -- the system asserted something false about the student.
     *
     * <p>Block instead of guessing: all three other exits are worse. Giving 0 is grading unfairly; skipping and
     * finalizing publishes an attempt with a missing score (exactly the bug this branch exists to patch); calling
     * the AI to grade it contradicts the decision "unblocking does not let the AI regrade". What is missing here
     * is the grader's job, so say plainly that it is missing.
     */
    private void requireNoAnsweredButUngraded(List<AssessmentItemResponse> ungraded) {
        long answeredButUngraded = ungraded.stream()
            .filter(response -> !missingResponseBackfillService.isSilentAnswer(response))
            .count();
        if (answeredButUngraded > 0) {
            throw new IllegalArgumentException(
                "Còn " + answeredButUngraded + " câu thí sinh có trả lời nhưng chưa được chấm. "
                    + "Hãy chấm những câu đó trước khi giữ nguyên điểm."
            );
        }
    }
}
