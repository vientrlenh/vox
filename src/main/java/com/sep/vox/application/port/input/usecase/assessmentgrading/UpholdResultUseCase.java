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

import com.sep.vox.application.port.input.command.GradingDecisionCommand;
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

        // Gỡ cờ nghi vấn: giáo viên đã xem và kết luận không vi phạm. Giữ nguyên
        // flagReason để còn tra lại vì sao bài từng bị đánh dấu.
        //
        // Ở REMEDIATION thì KHÔNG gỡ: giữ nguyên ở vòng đó nghĩa là xác nhận vi phạm,
        // gỡ cờ lúc này sẽ nói ngược lại chính quyết định vừa ghi.
        AssessmentSession session = prepared.context().session();
        if (prepared.roundType() != GradingRoundType.REMEDIATION && session.isFlagged()) {
            session.setFlagged(false);
            assessmentSessionRepository.save(session);
        }

        AssessmentCandidateResult result = prepared.context().candidateResult();
        AssessmentCandidateResultStatus targetStatus = GradingRoundPolicy.resultStatusAfter(
            prepared.roundType(), GradingOutcome.UPHELD);

        // The "must be fully graded" guard below applies ONLY to the rounds where keeping the score means PUBLISHING
        // (INITIAL, APPEAL -- see the matrix in GradingRoundPolicy). That is where a missing score leaks out
        // to the student, so it is also the only place worth blocking.
        //
        // The REMEDIATION round is outside it: the attempt there is INVALID and stays INVALID (targetStatus
        // null), and keeping it means CONFIRMING THE VIOLATION, not keeping a score. An attempt that was
        // forcibly ended during the session has by design no evaluation
        // (SubmitAssessmentSessionUseCase exits early in the blockedAt branch), so this guard would hit EXACTLY
        // the attempts the REMEDIATION round was created to handle: the teacher cannot grade it (there is
        // nothing to grade), cannot keep it as it is, and the assignment becomes a dead end with no way to close it.
        // The post-check (SPOT_CHECK) is also outside it because the attempt is already RELEASED -- blocking there recovers
        // nothing, it only locks out the person who is reviewing the attempt.
        boolean publishesResult = targetStatus == AssessmentCandidateResultStatus.RELEASED;

        boolean filledAny = publishesResult && fillSilentAnswersOrRefuse(result.getSessionId(), session);

        // Chỉ tính lại khi thật sự vừa điền -- bài đã chấm đủ giữ nguyên đường cũ, không đụng
        // tới điểm đang có.
        AssessmentCandidateResult finalResult = result;
        if (filledAny) {
            finalResult = upsertAssessmentCandidateResultUseCase.execute(
                result.getSessionId(),
                targetStatus == null ? result.getStatus() : targetStatus);
        }

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
        // A question the candidate NEVER attempted has no response row, so the loop below does not see
        // it. If it is not filled in, the total score is averaged over only the questions that WERE attempted -- an attempt of 2 questions that skipped 1
        // question gets 8 instead of 4. It has to be filled in BEFORE the early-exit branch below, because an attempt with no response
        // at all is the one that needs filling in the most.
        //
        // But only fill in when the session has really closed. GradingActionSupport.prepare only checks the status of the
        // ASSIGNMENT and the RESULT, not the status of the SESSION -- so a teacher can click keep-the-score
        // while the candidate is still in the middle of the attempt. That path is real: ClearInvalidResultUseCase
        // both clears blockedAt so the candidate can resume the attempt and opens the INITIAL round for the teacher to finalize.
        //
        // RESUMABLE = {IN_PROGRESS, INTERRUPTED} means exactly "the candidate can still come back and answer",
        // so that is the predicate to ask. Filling in early would repeat the two-responses-for-one-question bug that
        // ForceEndAssessmentSessionUseCase has just had to remove.
        if (!AssessmentSessionStatus.RESUMABLE.contains(session.getStatus())) {
            missingResponseBackfillService.backfill(sessionId, session.getPaperId());
        }

        List<AssessmentItemResponse> responses = assessmentItemResponseRepository.findBySessionId(sessionId);
        if (responses.isEmpty()) {
            return false;
        }
        Set<UUID> evaluatedResponseIds = assessmentItemEvaluationRepository
            .findByResponseIdIn(responses.stream().map(response -> response.getId()).toList()).stream()
            .map(evaluation -> evaluation.getResponseId())
            .collect(Collectors.toSet());
        List<AssessmentItemResponse> ungraded = responses.stream()
            .filter(response -> !evaluatedResponseIds.contains(response.getId()))
            .toList();

        // Chỉ câu THẬT SỰ rỗng mới được cho 0. Câu thí sinh có nói mà chưa ai chấm thì không
        // "giữ nguyên" được -- không có con điểm nào ở đó để mà giữ.
        //
        // Bản đầu của nhánh này cho 0 tất cả câu chưa có bản chấm, không hỏi câu đó có nội dung
        // hay không. Đo được 2026-08-17 trên phiên 01a0101d: một câu đã nói 2 lượt (41 giây +
        // 36 giây, đủ audio lẫn transcript) bị ghi 0 điểm kèm "Thí sinh không đưa ra câu trả
        // lời nào cho câu hỏi này" -- hệ thống phát ngôn một điều sai về học sinh.
        //
        // Chặn thay vì đoán: ba lối ra đều tệ hơn. Cho 0 là chấm oan; bỏ qua rồi chốt là công bố
        // bài thiếu điểm (đúng lỗi mà nhánh này sinh ra để vá); gọi AI chấm hộ thì mâu thuẫn với
        // quyết định "gỡ chặn không cho AI chấm lại". Việc còn thiếu ở đây là việc của người
        // chấm, nên nói thẳng ra là còn thiếu.
        long answeredButUngraded = ungraded.stream()
            .filter(response -> !missingResponseBackfillService.isSilentAnswer(response))
            .count();
        if (answeredButUngraded > 0) {
            throw new IllegalArgumentException(
                "Còn " + answeredButUngraded + " câu thí sinh có trả lời nhưng chưa được chấm. "
                    + "Hãy chấm những câu đó trước khi giữ nguyên điểm."
            );
        }

        boolean filledAny = false;
        for (AssessmentItemResponse response : ungraded) {
            filledAny |= missingResponseBackfillService.recordSilentAnswer(
                response.getId(), response.getPaperItemId());
        }
        return filledAny;
    }
}
