package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessment.DeleteAssessmentMemberCommand;
import com.sep.vox.application.port.input.service.AssessmentMemberManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

@Service
@RequiredArgsConstructor
public class DeleteAssessmentMemberUseCase implements IUseCase<DeleteAssessmentMemberCommand, Void> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentMemberManageAccessService assessmentMemberManageAccessService;

    @Override
    @Transactional
    public Void execute(DeleteAssessmentMemberCommand input) {
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        AssessmentMember member = assessmentMemberRepository.findById(input.memberId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment member not found"));

        if (!member.getAssessmentId().equals(assessment.getId())) {
            throw new ForbiddenException("Access denied");
        }

        AssessmentMemberManageAccessService.AssessmentMemberActor actor = assessmentMemberManageAccessService.requireCanManage(assessment);
        assessmentMemberManageAccessService.requireCanTouchRole(actor, member.getRole());

        assessmentMemberRepository.deleteById(member.getId());
        return null;
    }
}
