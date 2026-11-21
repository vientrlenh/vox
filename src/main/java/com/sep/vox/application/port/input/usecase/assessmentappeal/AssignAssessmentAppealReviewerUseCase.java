package com.sep.vox.application.port.input.usecase.assessmentappeal;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.AssessmentAppealApprovedPayloadV1;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.AssignAssessmentAppealReviewerCommand;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.ResultStatusChangeSource;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;
import com.sep.vox.domain.repository.OutboxRepository;

/**
 * Assigns ONE teacher to grade an appeal. The appeal moves {@code APPROVED -> GRADING}.
 *
 * <p>The appeal round is an ordinary {@code assessment_grading_assignments} row
 * ({@code roundType = APPEAL}, {@code appealId} points back to the appeal) -- there is no longer a separate
 * {@code assessment_appeal_reviewers} table. This way the teacher's queue, the grading deadline, overdue
 * reclaiming and the audit share exactly one mechanism with the other three rounds.
 *
 * <p><strong>Conflict-of-interest rule:</strong> whoever has ever written an {@code AssessmentItemEvaluation}
 * with engine HUMAN for this attempt may not grade its appeal -- they would have to re-inspect
 * their own verdict. Someone who only <em>confirmed</em> the old score ({@code UPHOLD}, without
 * writing a new version) is NOT excluded: they have no verdict of their own to defend. A small school
 * with too few teachers can have the admin override it, but a reason must be entered and that reason stays
 * on the appeal.
 */
@Service
public class AssignAssessmentAppealReviewerUseCase implements IUseCase<AssignAssessmentAppealReviewerCommand, UUID> {

    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public AssignAssessmentAppealReviewerUseCase(
            AssessmentResultAppealRepository assessmentResultAppealRepository,
            AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentGradingQueryRepository assessmentGradingQueryRepository,
            AssessmentAppealAccessService assessmentAppealAccessService,
            ResultStatusHistoryRecorder resultStatusHistoryRecorder,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.assessmentResultAppealRepository = assessmentResultAppealRepository;
        this.assessmentGradingAssignmentRepository = assessmentGradingAssignmentRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentGradingQueryRepository = assessmentGradingQueryRepository;
        this.assessmentAppealAccessService = assessmentAppealAccessService;
        this.resultStatusHistoryRecorder = resultStatusHistoryRecorder;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public UUID execute(AssignAssessmentAppealReviewerCommand command) {
        UUID currentUserId = assessmentAppealAccessService.requireActiveUserId();
        AssessmentAppealAccessService.AppealContext context = assessmentAppealAccessService.load(command.appealId());
        assessmentAppealAccessService.authorizeSchoolAdminOrClassTestChair(context, currentUserId);

        AssessmentResultAppeal appeal = context.appeal();
        if (appeal.getStatus() != AssessmentAppealStatus.APPROVED) {
            throw new IllegalStateException("Chỉ có thể phân công người chấm cho đơn phúc khảo đã được duyệt.");
        }
        if (command.reviewerId() == null) {
            throw new IllegalArgumentException("Thiếu người chấm phúc khảo.");
        }
        if (command.reviewerId().equals(context.studentId())) {
            throw new IllegalArgumentException("Không thể phân công chính thí sinh làm người chấm.");
        }
        if (!assessmentAppealAccessService.isTeacherOfSchool(command.reviewerId(), context.schoolId())) {
            throw new IllegalArgumentException("Người chấm phải là giáo viên thuộc cùng trường với bài thi.");
        }

        AssessmentCandidateResult candidateResult = context.candidateResult();
        // Bài đang có phân công khác chưa đóng thì không mở thêm được — unique index
        // trên active_result_id sẽ chặn, nhưng báo lỗi ở đây mới đọc được.
        if (assessmentGradingAssignmentRepository.findOpenByCandidateResultId(candidateResult.getId()).isPresent()) {
            throw new DuplicatedException("Bài thi này đang được một giáo viên chấm ở vòng khác.");
        }

        // Bài kiểm tra trên lớp: chính chủ bài tự nhận, nên luật xung đột lợi ích dưới
        // đây gần như chắc chắn bắn — và đó là chủ ý. Chỉ khi họ ĐÃ chấm tay bài này
        // (REGRADED sinh evaluation HUMAN) mới cần lý do; chỉ UPHELD thì không sinh
        // evaluation nên không có gì để xung đột.
        String overrideReason = normalize(command.overrideReason());
        Set<UUID> conflicted = assessmentGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResult.getId());
        if (conflicted.contains(command.reviewerId())) {
            if (overrideReason == null) {
                throw new IllegalArgumentException(
                    "Giáo viên này đã từng chấm bài thi này. Chọn người khác, "
                        + "hoặc nhập lý do nếu vẫn muốn phân công.");
            }
            appeal.setReviewerOverrideReason(overrideReason);
        }

        Instant now = Instant.now();
        Instant deadlineAt = command.deadlineAt() == null ? appeal.getDeadline() : command.deadlineAt();
        if (deadlineAt != null && deadlineAt.isBefore(now)) {
            throw new IllegalArgumentException("Hạn chấm phải ở tương lai.");
        }

        AssessmentGradingAssignment assignment = assessmentGradingAssignmentRepository.save(AssessmentGradingAssignment.open(
            candidateResult.getId(),
            command.reviewerId(),
            GradingRoundType.APPEAL,
            appeal.getId(),
            candidateResult.getTotalScore(),
            now,
            currentUserId,
            deadlineAt
        ));

        appeal.setStatus(AssessmentAppealStatus.GRADING);
        assessmentResultAppealRepository.save(appeal);

        ResultStatusHistoryRecorder.Snapshot before = resultStatusHistoryRecorder.snapshot(candidateResult);
        candidateResult.setStatus(AssessmentCandidateResultStatus.RE_GRADING);
        assessmentCandidateResultRepository.save(candidateResult);
        resultStatusHistoryRecorder.record(
            before, candidateResult, ResultStatusChangeSource.TEACHER_APPEAL, currentUserId, overrideReason);

        // Học sinh biết đơn đã được nhận và đang được chấm lại — mốc thông tin quan
        // trọng nhất giữa lúc nộp đơn và lúc có kết quả. Ghi outbox trong chính
        // transaction phân công để mail và phân công cùng sống hoặc cùng chết.
        AssessmentAppealApprovedPayloadV1 payload = new AssessmentAppealApprovedPayloadV1(
            appeal.getId(), context.studentId(), context.assessmentName(), deadlineAt);
        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.ASSESSMENT_RESULT_APPEAL,
            appeal.getId(),
            EventTypeConstant.ASSESSMENT_APPEAL_APPROVED,
            jsonSerializationPort.toJson(payload),
            now
        ));

        return assignment.getId();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
