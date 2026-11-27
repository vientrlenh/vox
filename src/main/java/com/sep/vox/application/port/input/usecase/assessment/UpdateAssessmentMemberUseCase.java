package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessment.UpdateAssessmentMemberCommand;
import com.sep.vox.application.port.input.service.AssessmentMemberManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.AssessmentMemberDto;
import com.sep.vox.domain.mapper.AssessmentMemberDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentMemberUseCase implements IUseCase<UpdateAssessmentMemberCommand, AssessmentMemberDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentMemberManageAccessService assessmentMemberManageAccessService;

    @Override
    @Transactional
    public AssessmentMemberDto execute(UpdateAssessmentMemberCommand input) {
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        AssessmentMember member = assessmentMemberRepository.findById(input.memberId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment member not found"));

        if (!member.getAssessmentId().equals(assessment.getId())) {
            throw new ForbiddenException("Access denied");
        }

        AssessmentMemberManageAccessService.AssessmentMemberActor actor = assessmentMemberManageAccessService.requireCanManage(assessment);
        // Changing a role touches two roles: the old one is removed and the new one is assigned. Check both, otherwise
        // a CHAIR could demote another CHAIR to AUTHOR just because the target role is not CHAIR.
        AssessmentMemberRole newRole = input.role();
        assessmentMemberManageAccessService.requireCanTouchRole(actor, member.getRole());
        assessmentMemberManageAccessService.requireCanTouchRole(actor, newRole);

        member.setRole(newRole);
        return AssessmentMemberDtoMapper.toDto(assessmentMemberRepository.save(member));
    }
}
