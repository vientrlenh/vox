package com.sep.vox.application.port.input.usecase.examgrading;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.InvalidateGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examgrading.InvalidateGradingResponse;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;

/**
 * Giáo viên kết luận bài nghi vấn là vi phạm thật -> result INVALID, không nhập điểm.
 *
 * <p>Cùng quyết định với {@code ReviewFlaggedExamResultUseCase} của admin, nhưng
 * phân quyền theo <em>dòng phân công</em> chứ không theo vai trò coi thi: giáo viên
 * được giao bài nào thì quyết được cờ của đúng bài đó. Hai đường chạy song song.
 */
@Service
public class InvalidateGradingUseCase implements IUseCase<InvalidateGradingCommand, InvalidateGradingResponse> {

    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public InvalidateGradingUseCase(
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional
    public InvalidateGradingResponse execute(InvalidateGradingCommand command) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var context = examGradingAccessService.loadForGrading(command.assignmentId(), command.candidateResultId());
        examGradingAccessService.authorizeGrader(context, currentUserId);

        var assignment = context.assignment();
        if (assignment != null && assignment.isCompleted()) {
            throw new IllegalStateException("Bạn đã chốt kết quả cho bài thi này.");
        }
        // Bài không bị đánh dấu thì không có gì để kết luận vi phạm — điểm kém là
        // việc của /grade, không phải của đường này.
        if (!context.session().isFlagged()) {
            throw new IllegalStateException("Chỉ có thể vô hiệu bài thi đã bị đánh dấu nghi vấn.");
        }
        if (context.candidateResult().getStatus() != ExamCandidateResultStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Bài thi này không còn ở trạng thái chờ chấm.");
        }

        var now = OffsetDateTime.now();
        var result = context.candidateResult();
        result.setStatus(ExamCandidateResultStatus.INVALID);
        result.setFinalizedAt(now);
        result.setUpdatedAt(now);
        result.setUpdatedBy(currentUserId);
        examCandidateResultRepository.save(result);

        if (assignment != null) {
            assignment.setStatus(GradingAssignmentStatus.COMPLETED);
            assignment.setCompletedAt(now);
            examGradingAssignmentRepository.save(assignment);
        }

        return new InvalidateGradingResponse(result.getId(), result.getStatus().name());
    }
}
