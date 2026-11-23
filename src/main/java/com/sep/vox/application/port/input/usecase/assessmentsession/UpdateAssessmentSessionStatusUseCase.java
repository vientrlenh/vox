package com.sep.vox.application.port.input.usecase.assessmentsession;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.SubmitAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.UpdateAssessmentSessionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentsession.AssessmentSessionResponse;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.repository.AiUsageRecordRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateAssessmentSessionStatusUseCase implements IUseCase<UpdateAssessmentSessionStatusCommand, AssessmentSessionResponse> {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AiUsageRecordRepository aiUsageRecordRepository;
    private final SubmitAssessmentSessionUseCase submitAssessmentSessionUseCase;

    @Override
    @Transactional
    public AssessmentSessionResponse execute(UpdateAssessmentSessionStatusCommand input) {
        AssessmentSession session = assessmentSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi"));

        // B.4: race - job hết-ca-thi vừa set EXPIRED ngay trước khi client kịp gửi SUBMITTED.
        // Coi là no-op thành công, giữ nguyên EXPIRED (việc chấm đã/sẽ diễn ra qua đường EXPIRED),
        // không throw để học sinh không thấy lỗi ngay lúc tưởng đã nộp bài xong.
        if (session.getStatus() == AssessmentSessionStatus.EXPIRED && input.status() == AssessmentSessionStatus.SUBMITTED) {
            return CreateAssessmentSessionUseCase.toResponse(session);
        }

        validateTransition(session.getStatus(), input.status());
        session.setStatus(input.status());
        if ((input.status() == AssessmentSessionStatus.SUBMITTED || input.status() == AssessmentSessionStatus.EXPIRED)
                && session.getSubmittedAt() == null) {
            session.setSubmittedAt(Instant.now());
        }
        applyGradingFailure(session, input);

        assessmentSessionRepository.save(session);
        if (input.status() == AssessmentSessionStatus.SUBMITTED && canSubmitImmediately(session)) {
            submitAssessmentSessionUseCase.execute(new SubmitAssessmentSessionCommand(session.getId()));
        }

        return CreateAssessmentSessionUseCase.toResponse(
            assessmentSessionRepository.findById(session.getId()).orElse(session)
        );
    }

    /**
     * The grading failure reason lives exactly as long as that failure.
     *
     * <p>LEAVING GRADING_FAILED means it must be cleared: the only way out is to GRADING (regrade), and
     * if that attempt succeeds while the old message stays behind, the triage page would still count this session in the
     * incident group -- and the on-call person would handle again exactly what has just been fixed. It is cleared right here, not waiting until
     * grading finishes, because between the two moments the session is GRADING so the
     * {@code chk_assessment_sessions_grading_error_only_when_failed} constraint in the DB would reject the whole record.
     *
     * <p>ENTERING GRADING_FAILED with a null {@code gradingFailure} also writes null: that is the DLT branch, and
     * "reason unknown" is the correct answer, not missing data.
     */
    private void applyGradingFailure(AssessmentSession session,
            UpdateAssessmentSessionStatusCommand input) {
        if (input.status() != AssessmentSessionStatus.GRADING_FAILED) {
            session.clearGradingFailure();
            return;
        }
        session.setGradingError(input.gradingFailure() == null ? null : input.gradingFailure().error());
        session.setGradingRetryCount(input.gradingFailure() == null ? null : input.gradingFailure().retryCount());
        waiveFailedRoundCost(session.getId());
    }

    /**
     * The grading attempt that just failed must NOT be charged to the school.
     *
     * <p>The rule is "charge for the work that PRODUCES A USABLE RESULT", not "forgive what fails". A failed
     * attempt leaves no {@code assessment_candidate_results} row, so the school received nothing --
     * very different from a speaking practice turn that has already answered the student, which is still charged whatever happens afterwards
     * (see {@code SubmitPracticeTurnUseCase}).
     *
     * <p>Waived RIGHT HERE rather than at regrading time: leaving it until then makes the money depend on whether someone
     * goes to fix it -- neglect it and it is free, fix it and you are charged for the failed attempt too. Exactly the
     * behavior we want to encourage would be the only one that is penalized.
     *
     * <p>Only touches rows that are NOT YET SETTLED. A successful grading attempt before (if any) has already stamped
     * {@code charged_at} and must be kept as it is: waiving over them would refund work that was fully delivered.
     */
    private void waiveFailedRoundCost(UUID sessionId) {
        int waived = aiUsageRecordRepository.markWaivedByAssessmentSessionId(sessionId, Instant.now());
        if (waived > 0) {
            log.info("Miễn {} dòng chi phí AI của lượt chấm hỏng ở phiên {}", waived, sessionId);
        }
    }

    /**
     * B.0: chỉ blockedAt quyết định hoãn/chấm ngay - flagged chỉ ảnh hưởng việc
     * có tính vào kết quả chính thức hay không (mục G/I), không hoãn việc chấm AI.
     */
    private boolean canSubmitImmediately(AssessmentSession session) {
        return assessmentCandidateRepository.findById(session.getCandidateId())
            .map(candidate -> candidate.getBlockedAt() == null)
            .orElse(false);
    }

    private void validateTransition(AssessmentSessionStatus from, AssessmentSessionStatus to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Trạng thái phiên thi không hợp lệ");
        }
        if (from == to) {
            return;
        }
        if (isAllowedTransition(from, to)) {
            return;
        }
        throw new IllegalArgumentException("Không thể chuyển trạng thái phiên thi từ " + from + " sang " + to);
    }

    private boolean isAllowedTransition(AssessmentSessionStatus from, AssessmentSessionStatus to) {
        return switch (from) {
            case IN_PROGRESS -> to == AssessmentSessionStatus.SUBMITTED
                || to == AssessmentSessionStatus.INTERRUPTED
                || to == AssessmentSessionStatus.EXPIRED;
            case INTERRUPTED -> to == AssessmentSessionStatus.IN_PROGRESS || to == AssessmentSessionStatus.EXPIRED;
            case SUBMITTED -> to == AssessmentSessionStatus.GRADING;
            case EXPIRED -> to == AssessmentSessionStatus.GRADING;
            case GRADING -> to == AssessmentSessionStatus.GRADED || to == AssessmentSessionStatus.GRADING_FAILED;
            case GRADING_FAILED -> to == AssessmentSessionStatus.GRADING;
            case GRADED -> to == AssessmentSessionStatus.GRADING;
            // DELETED là điểm dừng, không đi tiếp đâu được: phục hồi một phiên đã xoá là thao tác
            // sửa dữ liệu trực tiếp (xoá mềm không có đường phục hồi qua API), không phải một bước
            // chuyển trạng thái bình thường.
            case DELETED -> false;
        };
    }
}
