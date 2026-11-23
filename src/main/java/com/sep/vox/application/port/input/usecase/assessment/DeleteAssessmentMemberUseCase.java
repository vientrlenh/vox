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
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        AssessmentMember member = assessmentMemberRepository.findById(input.memberId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên bài kiểm tra"));

        if (!member.getAssessmentId().equals(assessment.getId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        AssessmentMemberManageAccessService.AssessmentMemberActor actor = assessmentMemberManageAccessService.requireCanManage(assessment);
        assessmentMemberManageAccessService.requireCanTouchRole(actor, member.getRole());

        assessmentMemberRepository.deleteById(member.getId());
        return null;
    }
}
