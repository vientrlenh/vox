package com.sep.vox.application.port.input.usecase.examappeal;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.ExamAppealApprovedPayloadV1;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.AssignExamAppealReviewerCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.model.exam.ResultStatusChangeSource;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.OutboxRepository;

/**
 * Giao MỘT giáo viên chấm phúc khảo. Đơn chuyển {@code APPROVED -> GRADING}.
 *
 * <p>Vòng phúc khảo là một dòng {@code exam_grading_assignments} bình thường
 * ({@code roundType = APPEAL}, {@code appealId} trỏ về đơn) — không còn bảng
 * {@code exam_appeal_reviewers} riêng. Nhờ vậy hàng đợi của giáo viên, hạn chấm, thu
 * hồi quá hạn và audit dùng chung đúng một cơ chế với ba vòng còn lại.
 *
 * <p><strong>Luật xung đột lợi ích:</strong> ai đã từng ghi {@code ExamItemEvaluation}
 * engine HUMAN cho bài này thì không được chấm phúc khảo — họ sẽ phải ngồi soi lại
 * chính phán quyết của mình. Người chỉ <em>xác nhận</em> điểm cũ ({@code UPHOLD}, không
 * ghi bản mới) KHÔNG bị loại: họ chưa ra phán quyết riêng nào để tự bảo vệ. Trường nhỏ
 * không đủ giáo viên thì admin override được, nhưng phải nhập lý do và lý do đó nằm
 * lại trên đơn.
 */
@Service
public class AssignExamAppealReviewerUseCase implements IUseCase<AssignExamAppealReviewerCommand, UUID> {

    private final ExamResultAppealRepository examResultAppealRepository;
    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamAppealAccessService examAppealAccessService;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public AssignExamAppealReviewerUseCase(
            ExamResultAppealRepository examResultAppealRepository,
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamAppealAccessService examAppealAccessService,
            ResultStatusHistoryRecorder resultStatusHistoryRecorder,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.examResultAppealRepository = examResultAppealRepository;
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examAppealAccessService = examAppealAccessService;
        this.resultStatusHistoryRecorder = resultStatusHistoryRecorder;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public UUID execute(AssignExamAppealReviewerCommand command) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        var context = examAppealAccessService.load(command.appealId());
        examAppealAccessService.authorizeSchoolAdmin(context, currentUserId);

        var appeal = context.appeal();
        if (appeal.getStatus() != ExamAppealStatus.APPROVED) {
            throw new IllegalStateException("Chỉ có thể phân công người chấm cho đơn phúc khảo đã được duyệt.");
        }
        if (command.reviewerId() == null) {
            throw new IllegalArgumentException("Thiếu người chấm phúc khảo.");
        }
        if (command.reviewerId().equals(context.studentId())) {
            throw new IllegalArgumentException("Không thể phân công chính thí sinh làm người chấm.");
        }
        if (!examAppealAccessService.isTeacherOfSchool(command.reviewerId(), context.schoolId())) {
            throw new IllegalArgumentException("Người chấm phải là giáo viên thuộc cùng trường với bài thi.");
        }

        var candidateResult = context.candidateResult();
        // Bài đang có phân công khác chưa đóng thì không mở thêm được — unique index
        // trên active_result_id sẽ chặn, nhưng báo lỗi ở đây mới đọc được.
        if (examGradingAssignmentRepository.findOpenByCandidateResultId(candidateResult.getId()).isPresent()) {
            throw new DuplicatedException("Bài thi này đang được một giáo viên chấm ở vòng khác.");
        }

        var overrideReason = normalize(command.overrideReason());
        var conflicted = examGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResult.getId());
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

        var assignment = examGradingAssignmentRepository.save(ExamGradingAssignment.open(
            candidateResult.getId(),
            command.reviewerId(),
            GradingRoundType.APPEAL,
            appeal.getId(),
            candidateResult.getTotalScore(),
            now,
            currentUserId,
            deadlineAt
        ));

        appeal.setStatus(ExamAppealStatus.GRADING);
        examResultAppealRepository.save(appeal);

        var before = resultStatusHistoryRecorder.snapshot(candidateResult);
        candidateResult.setStatus(ExamCandidateResultStatus.RE_GRADING);
        candidateResult.setUpdatedAt(now);
        candidateResult.setUpdatedBy(currentUserId);
        examCandidateResultRepository.save(candidateResult);
        resultStatusHistoryRecorder.record(
            before, candidateResult, ResultStatusChangeSource.TEACHER_APPEAL, currentUserId, overrideReason);

        // Học sinh biết đơn đã được nhận và đang được chấm lại — mốc thông tin quan
        // trọng nhất giữa lúc nộp đơn và lúc có kết quả. Ghi outbox trong chính
        // transaction phân công để mail và phân công cùng sống hoặc cùng chết.
        var payload = new ExamAppealApprovedPayloadV1(
            appeal.getId(), context.studentId(), context.examName(), deadlineAt);
        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.EXAM_RESULT_APPEAL,
            appeal.getId(),
            EventTypeConstant.EXAM_APPEAL_APPROVED,
            jsonSerializationPort.toJson(payload),
            now
        ));

        return assignment.getId();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
