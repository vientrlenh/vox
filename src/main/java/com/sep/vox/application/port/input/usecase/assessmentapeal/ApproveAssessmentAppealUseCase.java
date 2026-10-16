package com.sep.vox.application.port.input.usecase.assessmentapeal;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.ApproveAssessmentAppealCommand;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.repository.assessment.AssessmentResultAppealRepository;

@Service
public class ApproveAssessmentAppealUseCase implements IUseCase<ApproveAssessmentAppealCommand, UUID> {

    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;

    public ApproveAssessmentAppealUseCase(
            AssessmentResultAppealRepository assessmentResultAppealRepository,
            AssessmentAppealAccessService assessmentAppealAccessService) {
        this.assessmentResultAppealRepository = assessmentResultAppealRepository;
        this.assessmentAppealAccessService = assessmentAppealAccessService;
    }

    @Override
    @Transactional
    public UUID execute(ApproveAssessmentAppealCommand command) {
        var currentUserId = assessmentAppealAccessService.requireActiveUserId();
        var context = assessmentAppealAccessService.load(command.appealId());
        assessmentAppealAccessService.authorizeSchoolAdminOrClassTestChair(context, currentUserId);

        var appeal = context.appeal();
        if (appeal.getStatus() != AssessmentAppealStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể duyệt đơn phúc khảo đang ở trạng thái Chờ duyệt.");
        }

        var now = Instant.now();
        if (command.deadline() == null) {
            throw new IllegalArgumentException("Phải đặt hạn xử lý cho đơn phúc khảo.");
        }
        if (!command.deadline().isAfter(now)) {
            throw new IllegalArgumentException("Hạn xử lý phải ở tương lai.");
        }

        appeal.setStatus(AssessmentAppealStatus.APPROVED);
        appeal.setApprovedAt(now);
        appeal.setDeadline(command.deadline());
        assessmentResultAppealRepository.save(appeal);

        return appeal.getId();
    }
}
