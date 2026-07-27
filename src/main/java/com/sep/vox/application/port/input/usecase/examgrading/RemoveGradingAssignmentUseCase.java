package com.sep.vox.application.port.input.usecase.examgrading;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.RemoveGradingAssignmentCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

/**
 * Gỡ phân công = xoá dòng. Không có trạng thái "đã gỡ": bài quay lại đúng tình trạng
 * chưa gán, và unique index trên {@code active_result_id} lại trống để gán người khác.
 *
 * <p>Là một trong ba đường đóng phân công KHÔNG đi qua
 * {@code GradingActionSupport.finish()}, nên phần nhả đơn phúc khảo phải tự làm ở đây.
 */
@Service
public class RemoveGradingAssignmentUseCase implements IUseCase<RemoveGradingAssignmentCommand, UUID> {

    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamResultAppealRepository examResultAppealRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public RemoveGradingAssignmentUseCase(
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamResultAppealRepository examResultAppealRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examResultAppealRepository = examResultAppealRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional
    public UUID execute(RemoveGradingAssignmentCommand command) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var context = examGradingAccessService.load(command.assignmentId());
        examGradingAccessService.authorizeSchoolAdmin(context.schoolId(), currentUserId);

        var assignment = context.assignment();
        // Đã chấm xong thì điểm đã ghi và bài đã công bố — xoá dòng phân công lúc
        // này chỉ xoá mất vết ai chấm, không hoàn tác được gì.
        if (assignment.isCompleted()) {
            throw new IllegalStateException("Không thể gỡ phân công của bài đã chấm xong.");
        }

        // Xoá dòng là mất luôn con trỏ tới đơn phúc khảo, nên phải nhả đơn TRƯỚC:
        // đơn kẹt ở GRADING thì không giao lại được (chỉ nhận APPROVED) mà cũng không
        // duyệt lại được (chỉ nhận PENDING) — chỉ còn đường sửa tay dưới DB.
        if (assignment.getRoundType() == GradingRoundType.APPEAL && assignment.getAppealId() != null) {
            examResultAppealRepository.findById(assignment.getAppealId())
                .filter(appeal -> appeal.getStatus() == ExamAppealStatus.GRADING)
                .ifPresent(appeal -> {
                    appeal.setStatus(ExamAppealStatus.APPROVED);
                    examResultAppealRepository.save(appeal);
                });
        }

        examGradingAssignmentRepository.deleteById(command.assignmentId());
        return command.assignmentId();
    }
}
