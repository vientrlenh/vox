package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.AssessmentAppealPublishedPayloadV1;
import com.sep.vox.application.event.AssessmentResultRegradedPayloadV1;
import com.sep.vox.application.event.AssessmentResultReleasedPayloadV1;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService.GradingContext;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundPolicy;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.ResultStatusChangeSource;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;
import com.sep.vox.domain.repository.OutboxRepository;

/**
 * Phần dùng chung của bốn hành động chấm bài: mở đầu (phân quyền + kiểm luật vòng ×
 * hành động) và kết thúc (đổi trạng thái bài, ghi audit, đóng phân công, phát sự kiện).
 *
 * <p>Tồn tại vì bốn use case chỉ khác nhau ở đúng phần giữa. Nếu để mỗi use case tự
 * viết cả preamble lẫn postamble thì bốn bản sao sẽ trôi khỏi nhau, và cái trôi xa
 * nhất sẽ là cái quên ghi audit.
 */
@Service
@RequiredArgsConstructor
public class GradingActionSupport {

    private final AssessmentGradingAccessService assessmentGradingAccessService;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    /** Ngữ cảnh đã qua mọi kiểm tra, kèm ảnh chụp trạng thái trước khi thao tác. */
    public record PreparedAction(
        GradingContext context,
        UUID currentUserId,
        GradingRoundType roundType,
        GradingOutcome outcome,
        String reason,
        ResultStatusHistoryRecorder.Snapshot before
    ) {
    }

    /**
     * Phân quyền + kiểm luật, chụp ảnh trạng thái trước. Gọi TRƯỚC mọi thay đổi.
     *
     * @throws IllegalStateException khi phân công đã chốt, hành động không hợp lệ với
     *         vòng, hoặc bài đã rời khỏi trạng thái mà vòng này xử lý
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public PreparedAction prepare(UUID assignmentId, GradingOutcome outcome, String reason) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        // Khoá ngay từ đây: mọi kiểm tra bên dưới đều đọc-rồi-quyết-định, nên chúng chỉ
        // đúng nếu không ai chen vào giữa lúc đọc và lúc finish() ghi.
        AssessmentGradingAccessService.GradingContext context = assessmentGradingAccessService.loadForUpdate(assignmentId);
        assessmentGradingAccessService.authorizeAssignedTeacher(context, currentUserId);

        AssessmentGradingAssignment assignment = context.assignment();
        if (assignment.isCompleted()) {
            throw new IllegalStateException("Bạn đã chốt kết quả cho bài thi này.");
        }
        GradingRoundType roundType = assignment.getRoundType();
        if (roundType == null) {
            throw new IllegalStateException("Phân công này thiếu vòng chấm.");
        }
        if (!GradingRoundPolicy.isAllowed(roundType, outcome)) {
            throw new IllegalStateException("Hành động này không hợp lệ với vòng chấm hiện tại.");
        }
        // Bài có thể đã bị luồng khác kéo đi (phúc khảo, chốt sổ) trong lúc giáo viên
        // đang mở màn chấm; lúc đó phân công vẫn ASSIGNED nhưng không còn gì để làm.
        if (!GradingRoundPolicy.isAssignable(roundType, context.candidateResult().getStatus())) {
            throw new IllegalStateException("Bài thi này không còn ở trạng thái xử lý được của vòng chấm.");
        }
        String normalizedReason = reason == null || reason.isBlank() ? null : reason.trim();
        if (GradingRoundPolicy.requiresReason(outcome) && normalizedReason == null) {
            throw new IllegalArgumentException("Phải nhập lý do cho quyết định này.");
        }

        return new PreparedAction(
            context,
            currentUserId,
            roundType,
            outcome,
            normalizedReason,
            resultStatusHistoryRecorder.snapshot(context.candidateResult())
        );
    }

    /**
     * Đóng phân công, đổi trạng thái bài theo ma trận vòng × hành động, ghi audit và
     * phát sự kiện. Gọi SAU khi phần riêng của từng hành động đã ghi xong dữ liệu.
     *
     * @param result kết quả đã được use case cập nhật (điểm mới sau khi tính lại), hoặc
     *               chính {@code context.candidateResult()} khi hành động không đổi điểm
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void finish(PreparedAction prepared, AssessmentCandidateResult result) {
        Instant now = Instant.now();
        AssessmentCandidateResultStatus targetStatus = GradingRoundPolicy.resultStatusAfter(prepared.roundType(), prepared.outcome());

        if (targetStatus != null && result.getStatus() != targetStatus) {
            result.setStatus(targetStatus);
            if (targetStatus == AssessmentCandidateResultStatus.RELEASED && result.getReleasedAt() == null) {
                result.setReleasedAt(now);
            }
            if (targetStatus == AssessmentCandidateResultStatus.INVALID) {
                result.setFinalizedAt(now);
            }
            // Gỡ vô hiệu kéo bài về PENDING_REVIEW: mốc "đã chốt" của lần vô hiệu
            // trước không còn đúng nữa. Để lại thì tồn tại bài PENDING_REVIEW mang
            // finalized_at — một cái bẫy cho người đọc sau.
            if (targetStatus == AssessmentCandidateResultStatus.PENDING_REVIEW) {
                result.setFinalizedAt(null);
            }
            assessmentCandidateResultRepository.save(result);
        }

        // Quyết định "giữ nguyên" ở REMEDIATION (xác nhận vi phạm) và việc trả lại
        // phân công đều phải để lại dấu vết dù trạng thái đứng yên.
        ResultStatusChangeSource source = ResultStatusChangeSource.ofRound(prepared.roundType());
        if (prepared.outcome() == GradingOutcome.UPHELD || prepared.outcome() == GradingOutcome.DECLINED) {
            resultStatusHistoryRecorder.recordAlways(
                prepared.before(), result, source, prepared.currentUserId(), prepared.reason());
        } else {
            resultStatusHistoryRecorder.record(
                prepared.before(), result, source, prepared.currentUserId(), prepared.reason());
        }

        AssessmentGradingAssignment assignment = prepared.context().assignment();
        assignment.complete(prepared.outcome(), prepared.reason(), now);
        assessmentGradingAssignmentRepository.save(assignment);

        if (prepared.roundType() == GradingRoundType.APPEAL) {
            publishAppeal(prepared, result, now);
        }
        publishScoreEvents(prepared, result, now);
    }

    /**
     * Giáo viên nộp là ĐƠN ĐƯỢC CÔNG BỐ luôn — không còn bước admin đối chiếu rồi
     * publish. Đây là chỗ duy nhất đóng đơn phúc khảo, nên nó nằm cạnh chỗ đóng phân
     * công thay vì rải vào từng use case hành động.
     *
     * <p>{@code DECLINED} không công bố gì: giáo viên trả bài lại thì đơn vẫn đang chờ
     * người khác, chỉ lùi về {@code APPROVED} để admin giao lại.
     */
    private void publishAppeal(PreparedAction prepared, AssessmentCandidateResult result, Instant now) {
        UUID appealId = prepared.context().assignment().getAppealId();
        if (appealId == null) {
            return;
        }
        AssessmentResultAppeal appeal = assessmentResultAppealRepository.findById(appealId).orElse(null);
        if (appeal == null) {
            return;
        }
        if (prepared.outcome() == GradingOutcome.DECLINED) {
            appeal.setStatus(AssessmentAppealStatus.APPROVED);
            assessmentResultAppealRepository.save(appeal);
            return;
        }
        appeal.setStatus(AssessmentAppealStatus.PUBLISHED);
        appeal.setScoreAfter(result.getTotalScore());
        appeal.setResolvedBy(prepared.currentUserId());
        appeal.setResolvedAt(now);
        appeal.setDecisionNote(prepared.reason());
        assessmentResultAppealRepository.save(appeal);

        UUID studentId = resolveStudentId(result);
        if (studentId != null) {
            saveOutbox(
                AggregateTypeConstant.ASSESSMENT_RESULT_APPEAL,
                appeal.getId(),
                EventTypeConstant.ASSESSMENT_APPEAL_PUBLISHED,
                new AssessmentAppealPublishedPayloadV1(
                    appeal.getId(),
                    studentId,
                    prepared.context().assessmentName(),
                    appeal.getScoreBefore(),
                    result.getTotalScore()
                ),
                now
            );
        }
    }

    /**
     * Hai mail khác nhau cho hai tình huống khác nhau: lần đầu có điểm, và điểm đã
     * công bố nay đổi. Hậu kiểm giữ nguyên điểm thì KHÔNG phát gì — làm phiền học sinh
     * bằng thông báo "điểm vừa thay đổi" trong khi nó không đổi là sai.
     */
    private void publishScoreEvents(PreparedAction prepared, AssessmentCandidateResult result, Instant now) {
        UUID studentId = resolveStudentId(result);
        if (studentId == null) {
            return;
        }
        String assessmentName = prepared.context().assessmentName();
        boolean wasPublished = prepared.before().status() != AssessmentCandidateResultStatus.PENDING_REVIEW;
        boolean nowPublished = result.getStatus() == AssessmentCandidateResultStatus.RELEASED;

        if (nowPublished && !wasPublished) {
            saveOutbox(
                AggregateTypeConstant.ASSESSMENT_CANDIDATE_RESULT,
                result.getId(),
                EventTypeConstant.ASSESSMENT_RESULT_RELEASED,
                new AssessmentResultReleasedPayloadV1(
                    result.getId(), studentId, assessmentName, result.getTotalScore(),
                    result.getSessionId(), prepared.context().assessmentKind()),
                now
            );
            return;
        }
        boolean scoreChanged = prepared.before().totalScore() == null
            ? result.getTotalScore() != null
            : result.getTotalScore() != null
                && prepared.before().totalScore().compareTo(result.getTotalScore()) != 0;
        if (wasPublished && scoreChanged) {
            saveOutbox(
                AggregateTypeConstant.ASSESSMENT_CANDIDATE_RESULT,
                result.getId(),
                EventTypeConstant.ASSESSMENT_RESULT_REGRADED,
                new AssessmentResultRegradedPayloadV1(
                    result.getId(),
                    studentId,
                    assessmentName,
                    prepared.roundType().name(),
                    prepared.before().totalScore(),
                    result.getTotalScore(),
                    result.getSessionId(),
                    prepared.context().assessmentKind()
                ),
                now
            );
        }
    }

    /**
     * Ghi vào outbox trong CHÍNH transaction đang đổi trạng thái bài: mail và dữ liệu
     * cùng sống hoặc cùng chết, không còn cảnh commit xong rồi mất mail vì SMTP lỗi.
     */
    private void saveOutbox(String aggregateType, UUID aggregateId, String eventType, Object payload, Instant now) {
        outboxRepository.save(Outbox.create(
            aggregateType,
            aggregateId,
            eventType,
            jsonSerializationPort.toJson(payload),
            now
        ));
    }

    /** The student who receives the email is {@code AssessmentCandidate.studentId}, not the candidateId. */
    public UUID resolveStudentId(AssessmentCandidateResult result) {
        if (result.getCandidateId() == null) {
            return null;
        }
        return assessmentCandidateRepository.findById(result.getCandidateId())
            .map(candidate -> candidate.getStudentId())
            .orElse(null);
    }

    /** Bắt buộc phải có, dùng khi thiếu học sinh là lỗi dữ liệu chứ không phải ca biên. */
    public UUID requireStudentId(AssessmentCandidateResult result) {
        UUID studentId = resolveStudentId(result);
        if (studentId == null) {
            throw new NotFoundException("Không tìm thấy thí sinh của bài thi.");
        }
        return studentId;
    }
}
