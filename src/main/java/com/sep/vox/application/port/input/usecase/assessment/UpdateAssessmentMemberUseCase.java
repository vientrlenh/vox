package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.UpdateAssessmentMemberCommand;
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
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        AssessmentMember member = assessmentMemberRepository.findById(input.memberId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên bài kiểm tra"));

        if (!member.getAssessmentId().equals(assessment.getId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        AssessmentMemberManageAccessService.AssessmentMemberActor actor = assessmentMemberManageAccessService.requireCanManage(assessment);
        // Đổi vai trò chạm tới hai vai: vai cũ bị gỡ và vai mới được gán. Kiểm cả hai, nếu không thì
        // chủ tịch hội đồng hạ cấp được chủ tịch khác xuống AUTHOR chỉ vì vai đích không phải CHAIR.
        AssessmentMemberRole newRole = input.role();
        assessmentMemberManageAccessService.requireCanTouchRole(actor, member.getRole());
        assessmentMemberManageAccessService.requireCanTouchRole(actor, newRole);

        member.setRole(newRole);
        return AssessmentMemberDtoMapper.toDto(assessmentMemberRepository.save(member));
    }
}
