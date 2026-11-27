package com.sep.vox.application.port.input.usecase.assessmentappeal;

import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.assessmentappeal.ApproveAssessmentAppealCommand;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;

@Service
@RequiredArgsConstructor
public class ApproveAssessmentAppealUseCase implements IUseCase<ApproveAssessmentAppealCommand, UUID> {

    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;

    @Override
    @Transactional
    public UUID execute(ApproveAssessmentAppealCommand command) {
        UUID currentUserId = assessmentAppealAccessService.requireActiveUserId();
        AssessmentAppealAccessService.AppealContext context = assessmentAppealAccessService.load(command.appealId());
        assessmentAppealAccessService.authorizeSchoolAdminOrClassTestChair(context, currentUserId);

        AssessmentResultAppeal appeal = context.appeal();
        if (appeal.getStatus() != AssessmentAppealStatus.PENDING) {
            throw new IllegalArgumentException("Only an appeal in the Pending status can be approved.");
        }

        Instant now = Instant.now();
        if (command.deadline() == null) {
            throw new IllegalArgumentException("A processing deadline must be set for the appeal.");
        }
        if (!command.deadline().isAfter(now)) {
            throw new IllegalArgumentException("The processing deadline must be in the future.");
        }

        appeal.setStatus(AssessmentAppealStatus.APPROVED);
        appeal.setApprovedAt(now);
        appeal.setDeadline(command.deadline());
        assessmentResultAppealRepository.save(appeal);

        return appeal.getId();
    }
}
