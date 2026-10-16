package com.sep.vox.application.port.input.usecase.assessmentapeal;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.assessment.AssessmentAppealApprovedPayload;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.port.input.command.AssignAssessmentAppealReviewerCommand;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.projection.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentGradingRoundType;
import com.sep.vox.domain.model.assessment.AssessmentResultStatusChangeSource;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentResultAppealRepository;
import com.sep.vox.domain.shared.AggregateTypeConstant;
import com.sep.vox.domain.shared.EventTypeConstant;

/**
 * Giao MỘT giáo viên chấm phúc khảo. Đơn chuyển {@code APPROVED -> GRADING}.
 *
 * <p>Vòng phúc khảo là một dòng {@code assessment_grading_assignments} bình thường
 * ({@code roundType = APPEAL}, {@code appealId} trỏ về đơn) — không còn bảng
 * {@code assessment_appeal_reviewers} riêng. Nhờ vậy hàng đợi của giáo viên, hạn chấm, thu
 * hồi quá hạn và audit dùng chung đúng một cơ chế với ba vòng còn lại.
 *
 * <p><strong>Luật xung đột lợi ích:</strong> ai đã từng ghi {@code AssessmentItemEvaluation}
 * engine HUMAN cho bài này thì không được chấm phúc khảo — họ sẽ phải ngồi soi lại
 * chính phán quyết của mình. Người chỉ <em>xác nhận</em> điểm cũ ({@code UPHOLD}, không
 * ghi bản mới) KHÔNG bị loại: họ chưa ra phán quyết riêng nào để tự bảo vệ. Trường nhỏ
 * không đủ giáo viên thì admin override được, nhưng phải nhập lý do và lý do đó nằm
 * lại trên đơn.
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
        var currentUserId = assessmentAppealAccessService.requireActiveUserId();
        var context = assessmentAppealAccessService.load(command.appealId());
        assessmentAppealAccessService.authorizeSchoolAdminOrClassTestChair(context, currentUserId);

        var appeal = context.appeal();
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

        var candidateResult = context.candidateResult();
        // Bài đang có phân công khác chưa đóng thì không mở thêm được — unique index
        // trên active_result_id sẽ chặn, nhưng báo lỗi ở đây mới đọc được.
        if (assessmentGradingAssignmentRepository.findOpenByCandidateResultId(candidateResult.getId()).isPresent()) {
            throw new ResourceDuplicatedException("Bài thi này đang được một giáo viên chấm ở vòng khác.");
        }

        // Bài kiểm tra trên lớp: chính chủ bài tự nhận, nên luật xung đột lợi ích dưới
        // đây gần như chắc chắn bắn — và đó là chủ ý. Chỉ khi họ ĐÃ chấm tay bài này
        // (REGRADED sinh evaluation HUMAN) mới cần lý do; chỉ UPHELD thì không sinh
        // evaluation nên không có gì để xung đột.
        var overrideReason = normalize(command.overrideReason());
        var conflicted = assessmentGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResult.getId());
        if (conflicted.contains(command.reviewerId())) {
            if (overrideReason == null) {
                throw new IllegalArgumentException(
                    "Giáo viên này đã từng chấm bài thi này. Chọn người khác, "
                        + "hoặc nhập lý do nếu vẫn muốn phân công.");
            }
            appeal.setReviewerOverrideReason(overrideReason);
        }

        var now = Instant.now();
        var deadlineAt = command.deadlineAt() == null ? appeal.getDeadline() : command.deadlineAt();
        if (deadlineAt != null && deadlineAt.isBefore(now)) {
            throw new IllegalArgumentException("Hạn chấm phải ở tương lai.");
        }

        var assignment = assessmentGradingAssignmentRepository.save(AssessmentGradingAssignment.open(
            candidateResult.getId(),
            command.reviewerId(),
            AssessmentGradingRoundType.APPEAL,
            appeal.getId(),
            candidateResult.getTotalScore(),
            now,
            currentUserId,
            deadlineAt
        ));

        appeal.setStatus(AssessmentAppealStatus.GRADING);
        assessmentResultAppealRepository.save(appeal);

        var before = resultStatusHistoryRecorder.snapshot(candidateResult);
        candidateResult.setStatus(AssessmentCandidateResultStatus.RE_GRADING);
        candidateResult.setUpdatedAt(now);
        candidateResult.setUpdatedBy(currentUserId);
        assessmentCandidateResultRepository.save(candidateResult);
        resultStatusHistoryRecorder.record(
            before, candidateResult, AssessmentResultStatusChangeSource.TEACHER_APPEAL, currentUserId, overrideReason);

        // Học sinh biết đơn đã được nhận và đang được chấm lại — mốc thông tin quan
        // trọng nhất giữa lúc nộp đơn và lúc có kết quả. Ghi outbox trong chính
        // transaction phân công để mail và phân công cùng sống hoặc cùng chết.
        var payload = new AssessmentAppealApprovedPayload(
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
