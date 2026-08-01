package com.sep.vox.application.port.input.usecase.examgrading;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.ExamResultInvalidClearedPayloadV1;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.GradingDecisionCommand;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.response.input.examgrading.GradingActionResponse;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.OutboxRepository;

/**
 * Giáo viên soi lại bài bị vô hiệu và kết luận KHÔNG vi phạm: bài về PENDING_REVIEW,
 * thí sinh được gỡ chặn, và một vòng {@code INITIAL} mới mở ngay cho chính giáo viên đó.
 *
 * <p><strong>Vì sao tự mở vòng mới:</strong> bài vừa về PENDING_REVIEW là bài chưa có
 * điểm hợp lệ, cần được chấm; người đang cầm bài và vừa đọc nó là người hợp lý nhất.
 * Quan trọng hơn, nó bịt luôn kịch bản của review BE-1: bài gỡ vô hiệu rơi vào pool
 * auto-assign rồi bị giao thêm người thứ hai.
 *
 * <p><strong>Vì sao gỡ {@code blocked_at} ở đây:</strong> {@code resolveDefaultStatus}
 * ép INVALID khi thí sinh còn bị chặn, nên không gỡ thì mọi lần tính lại điểm sau này
 * sẽ kéo bài về INVALID và quyết định của giáo viên bị vô hiệu hoá âm thầm.
 *
 * <p>Đây là chỗ DUY NHẤT giáo viên gỡ được chặn thí sinh (bình thường là quyền
 * SCHOOL_ADMIN qua {@code UnblockExamCandidateUseCase}). Phạm vi bị siết chặt: đúng
 * bài được giao vòng {@code REMEDIATION}, lý do bắt buộc, và mọi lần dùng đều vào audit.
 */
@Service
public class ClearInvalidResultUseCase implements IUseCase<GradingDecisionCommand, GradingActionResponse> {

    private final GradingActionSupport gradingActionSupport;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public ClearInvalidResultUseCase(
            GradingActionSupport gradingActionSupport,
            ExamCandidateRepository examCandidateRepository,
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.gradingActionSupport = gradingActionSupport;
        this.examCandidateRepository = examCandidateRepository;
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public GradingActionResponse execute(GradingDecisionCommand command) {
        var prepared = gradingActionSupport.prepare(
            command.assignmentId(), GradingOutcome.CLEARED_INVALID, command.reason());

        var result = prepared.context().candidateResult();
        var now = Instant.now();

        var candidate = examCandidateRepository.findById(result.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh của bài thi."));
        if (candidate.getBlockedAt() != null) {
            candidate.setBlockedAt(null);
            candidate.setUpdatedAt(now);
            candidate.setUpdatedBy(prepared.currentUserId());
            examCandidateRepository.save(candidate);
        }

        // finish() đóng dòng REMEDIATION và nhả active_result_id; chỉ sau đó mới mở
        // được vòng INITIAL cho cùng bài mà không đụng unique index.
        gradingActionSupport.finish(prepared, result);

        // Hạn của vòng REMEDIATION KHÔNG được kế thừa: vòng soi bài vô hiệu thường
        // được xử lý sát hoặc đã quá hạn, nên phân công mới sẽ sinh ra với một mốc đã
        // trôi qua — đỏ "quá hạn" ngay lúc tạo, job nhắc bắn ngay lượt sau, và lọt vào
        // danh sách thu hồi dù giáo viên chưa có một phút nào để chấm. Để trống, admin
        // đặt hạn sau qua PUT /grading-assignments/{id}/deadline.
        var next = examGradingAssignmentRepository.save(ExamGradingAssignment.open(
            result.getId(),
            prepared.currentUserId(),
            GradingRoundType.INITIAL,
            null,
            result.getTotalScore(),
            now,
            prepared.currentUserId(),
            null
        ));

        var studentId = gradingActionSupport.resolveStudentId(result);
        if (studentId != null) {
            var payload = new ExamResultInvalidClearedPayloadV1(
                result.getId(), studentId, prepared.context().examName(), prepared.reason());
            outboxRepository.save(Outbox.create(
                AggregateTypeConstant.EXAM_CANDIDATE_RESULT,
                result.getId(),
                EventTypeConstant.EXAM_RESULT_INVALID_CLEARED,
                jsonSerializationPort.toJson(payload),
                now
            ));
        }

        return new GradingActionResponse(
            command.assignmentId(),
            result.getId(),
            GradingOutcome.CLEARED_INVALID.name(),
            result.getStatus() == null ? null : result.getStatus().name(),
            result.getTotalScore(),
            next.getId()
        );
    }
}
