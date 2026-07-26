package com.sep.vox.application.port.input.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.ExamAppealPublishedEvent;
import com.sep.vox.application.event.ExamResultRegradedEvent;
import com.sep.vox.application.event.ExamResultReleasedEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.service.ExamGradingAccessService.GradingContext;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundPolicy;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.model.exam.ResultStatusChangeSource;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

/**
 * Phần dùng chung của bốn hành động chấm bài: mở đầu (phân quyền + kiểm luật vòng ×
 * hành động) và kết thúc (đổi trạng thái bài, ghi audit, đóng phân công, phát sự kiện).
 *
 * <p>Tồn tại vì bốn use case chỉ khác nhau ở đúng phần giữa. Nếu để mỗi use case tự
 * viết cả preamble lẫn postamble thì bốn bản sao sẽ trôi khỏi nhau, và cái trôi xa
 * nhất sẽ là cái quên ghi audit.
 */
@Service
public class GradingActionSupport {

    private final ExamGradingAccessService examGradingAccessService;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamResultAppealRepository examResultAppealRepository;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;
    private final EventPublisherPort eventPublisherPort;

    public GradingActionSupport(
            ExamGradingAccessService examGradingAccessService,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamResultAppealRepository examResultAppealRepository,
            ResultStatusHistoryRecorder resultStatusHistoryRecorder,
            EventPublisherPort eventPublisherPort) {
        this.examGradingAccessService = examGradingAccessService;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examResultAppealRepository = examResultAppealRepository;
        this.resultStatusHistoryRecorder = resultStatusHistoryRecorder;
        this.eventPublisherPort = eventPublisherPort;
    }

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
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var context = examGradingAccessService.load(assignmentId);
        examGradingAccessService.authorizeAssignedTeacher(context, currentUserId);

        var assignment = context.assignment();
        if (assignment.isCompleted()) {
            throw new IllegalStateException("Bạn đã chốt kết quả cho bài thi này.");
        }
        var roundType = assignment.getRoundType();
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
        var normalizedReason = reason == null || reason.isBlank() ? null : reason.trim();
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
    public void finish(PreparedAction prepared, ExamCandidateResult result) {
        var now = OffsetDateTime.now();
        var targetStatus = GradingRoundPolicy.resultStatusAfter(prepared.roundType(), prepared.outcome());

        if (targetStatus != null && result.getStatus() != targetStatus) {
            result.setStatus(targetStatus);
            if (targetStatus == ExamCandidateResultStatus.RELEASED && result.getReleasedAt() == null) {
                result.setReleasedAt(now);
            }
            if (targetStatus == ExamCandidateResultStatus.INVALID) {
                result.setFinalizedAt(now);
            }
            result.setUpdatedAt(now);
            result.setUpdatedBy(prepared.currentUserId());
            examCandidateResultRepository.save(result);
        }

        // Quyết định "giữ nguyên" ở REMEDIATION (xác nhận vi phạm) và việc trả lại
        // phân công đều phải để lại dấu vết dù trạng thái đứng yên.
        var source = ResultStatusChangeSource.ofRound(prepared.roundType());
        if (prepared.outcome() == GradingOutcome.UPHELD || prepared.outcome() == GradingOutcome.DECLINED) {
            resultStatusHistoryRecorder.recordAlways(
                prepared.before(), result, source, prepared.currentUserId(), prepared.reason());
        } else {
            resultStatusHistoryRecorder.record(
                prepared.before(), result, source, prepared.currentUserId(), prepared.reason());
        }

        var assignment = prepared.context().assignment();
        assignment.complete(prepared.outcome(), prepared.reason(), now);
        examGradingAssignmentRepository.save(assignment);

        if (prepared.roundType() == GradingRoundType.APPEAL) {
            publishAppeal(prepared, result, now);
        }
        publishScoreEvents(prepared, result);
    }

    /**
     * Giáo viên nộp là ĐƠN ĐƯỢC CÔNG BỐ luôn — không còn bước admin đối chiếu rồi
     * publish. Đây là chỗ duy nhất đóng đơn phúc khảo, nên nó nằm cạnh chỗ đóng phân
     * công thay vì rải vào từng use case hành động.
     *
     * <p>{@code DECLINED} không công bố gì: giáo viên trả bài lại thì đơn vẫn đang chờ
     * người khác, chỉ lùi về {@code APPROVED} để admin giao lại.
     */
    private void publishAppeal(PreparedAction prepared, ExamCandidateResult result, OffsetDateTime now) {
        var appealId = prepared.context().assignment().getAppealId();
        if (appealId == null) {
            return;
        }
        var appeal = examResultAppealRepository.findById(appealId).orElse(null);
        if (appeal == null) {
            return;
        }
        if (prepared.outcome() == GradingOutcome.DECLINED) {
            appeal.setStatus(ExamAppealStatus.APPROVED);
            examResultAppealRepository.save(appeal);
            return;
        }
        appeal.setStatus(ExamAppealStatus.PUBLISHED);
        appeal.setScoreAfter(result.getTotalScore());
        appeal.setResolvedBy(prepared.currentUserId());
        appeal.setResolvedAt(now);
        appeal.setDecisionNote(prepared.reason());
        examResultAppealRepository.save(appeal);

        var studentId = resolveStudentId(result);
        if (studentId != null) {
            eventPublisherPort.publish(new ExamAppealPublishedEvent(
                appeal.getId(),
                studentId,
                prepared.context().examName(),
                appeal.getScoreBefore(),
                result.getTotalScore()
            ));
        }
    }

    /**
     * Hai mail khác nhau cho hai tình huống khác nhau: lần đầu có điểm, và điểm đã
     * công bố nay đổi. Hậu kiểm giữ nguyên điểm thì KHÔNG phát gì — làm phiền học sinh
     * bằng thông báo "điểm vừa thay đổi" trong khi nó không đổi là sai.
     */
    private void publishScoreEvents(PreparedAction prepared, ExamCandidateResult result) {
        var studentId = resolveStudentId(result);
        if (studentId == null) {
            return;
        }
        var examName = prepared.context().examName();
        var wasPublished = prepared.before().status() != ExamCandidateResultStatus.PENDING_REVIEW;
        var nowPublished = result.getStatus() == ExamCandidateResultStatus.RELEASED;

        if (nowPublished && !wasPublished) {
            eventPublisherPort.publish(new ExamResultReleasedEvent(
                result.getId(), studentId, examName, result.getTotalScore()));
            return;
        }
        var scoreChanged = prepared.before().totalScore() == null
            ? result.getTotalScore() != null
            : result.getTotalScore() != null
                && prepared.before().totalScore().compareTo(result.getTotalScore()) != 0;
        if (wasPublished && scoreChanged) {
            eventPublisherPort.publish(new ExamResultRegradedEvent(
                result.getId(),
                studentId,
                examName,
                prepared.roundType().name(),
                prepared.before().totalScore(),
                result.getTotalScore()
            ));
        }
    }

    /** Học sinh nhận mail là {@code ExamCandidate.studentId}, không phải candidateId. */
    public UUID resolveStudentId(ExamCandidateResult result) {
        if (result.getCandidateId() == null) {
            return null;
        }
        return examCandidateRepository.findById(result.getCandidateId())
            .map(candidate -> candidate.getStudentId())
            .orElse(null);
    }

    /** Bắt buộc phải có, dùng khi thiếu học sinh là lỗi dữ liệu chứ không phải ca biên. */
    public UUID requireStudentId(ExamCandidateResult result) {
        var studentId = resolveStudentId(result);
        if (studentId == null) {
            throw new NotFoundException("Không tìm thấy thí sinh của bài thi.");
        }
        return studentId;
    }
}
