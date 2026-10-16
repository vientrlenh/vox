package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.RemoveGradingAssignmentCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingRoundType;
import com.sep.vox.domain.repository.assessment.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentResultAppealRepository;

/**
 * Gỡ phân công = xoá dòng. Không có trạng thái "đã gỡ": bài quay lại đúng tình trạng
 * chưa gán, và unique index trên {@code active_result_id} lại trống để gán người khác.
 *
 * <p>Là một trong ba đường đóng phân công KHÔNG đi qua
 * {@code GradingActionSupport.finish()}, nên phần nhả đơn phúc khảo phải tự làm ở đây.
 */
@Service
public class RemoveGradingAssignmentUseCase implements IUseCase<RemoveGradingAssignmentCommand, UUID> {

    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    public RemoveGradingAssignmentUseCase(
            AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository,
            AssessmentResultAppealRepository assessmentResultAppealRepository,
            AssessmentGradingAccessService assessmentGradingAccessService) {
        this.assessmentGradingAssignmentRepository = assessmentGradingAssignmentRepository;
        this.assessmentResultAppealRepository = assessmentResultAppealRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
    }

    @Override
    @Transactional
    public UUID execute(RemoveGradingAssignmentCommand command) {
        var currentUserId = assessmentGradingAccessService.requireActiveUserId();
        var context = assessmentGradingAccessService.load(command.assignmentId());
        assessmentGradingAccessService.authorizeSchoolAdmin(context.schoolId(), currentUserId);
        assessmentGradingAccessService.rejectClassTestCoordination(context.candidateResult().getId());

        var assignment = context.assignment();
        // Đã chấm xong thì điểm đã ghi và bài đã công bố — xoá dòng phân công lúc
        // này chỉ xoá mất vết ai chấm, không hoàn tác được gì.
        if (assignment.isCompleted()) {
            throw new IllegalStateException("Không thể gỡ phân công của bài đã chấm xong.");
        }

        // Xoá dòng là mất luôn con trỏ tới đơn phúc khảo, nên phải nhả đơn TRƯỚC:
        // đơn kẹt ở GRADING thì không giao lại được (chỉ nhận APPROVED) mà cũng không
        // duyệt lại được (chỉ nhận PENDING) — chỉ còn đường sửa tay dưới DB.
        if (assignment.getRoundType() == AssessmentGradingRoundType.APPEAL && assignment.getAppealId() != null) {
            assessmentResultAppealRepository.findById(assignment.getAppealId())
                .filter(appeal -> appeal.getStatus() == AssessmentAppealStatus.GRADING)
                .ifPresent(appeal -> {
                    appeal.setStatus(AssessmentAppealStatus.APPROVED);
                    assessmentResultAppealRepository.save(appeal);
                });
        }

        assessmentGradingAssignmentRepository.deleteById(command.assignmentId());
        return command.assignmentId();
    }
}
