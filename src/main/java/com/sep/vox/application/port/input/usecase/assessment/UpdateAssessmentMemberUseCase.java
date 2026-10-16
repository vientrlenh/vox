package com.sep.vox.application.port.input.usecase.assessment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.UpdateAssessmentMemberCommand;
import com.sep.vox.application.port.input.service.AssessmentMemberManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.dto.AssessmentMemberDto;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;

@Service
public class UpdateAssessmentMemberUseCase implements IUseCase<UpdateAssessmentMemberCommand, AssessmentMemberDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentMemberManageAccessService assessmentMemberManageAccessService;

    public UpdateAssessmentMemberUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            AssessmentMemberManageAccessService assessmentMemberManageAccessService) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.assessmentMemberManageAccessService = assessmentMemberManageAccessService;
    }

    @Override
    @Transactional
    public AssessmentMemberDto execute(UpdateAssessmentMemberCommand input) {
        var command = new UpdateAssessmentMemberCommand(
            input.assessmentId(),
            input.memberId(),
            StringNormalization.normalizeCode(input.role())
        );
        var assessment = assessmentRepository.findById(command.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        var member = assessmentMemberRepository.findById(command.memberId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên bài kiểm tra"));

        if (!member.getAssessmentId().equals(assessment.getId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var actor = assessmentMemberManageAccessService.requireCanManage(assessment);
        // Đổi vai trò chạm tới hai vai: vai cũ bị gỡ và vai mới được gán. Kiểm cả hai, nếu không thì
        // chủ tịch hội đồng hạ cấp được chủ tịch khác xuống AUTHOR chỉ vì vai đích không phải CHAIR.
        var newRole = AssessmentMemberRole.valueOf(command.role());
        assessmentMemberManageAccessService.requireCanTouchRole(actor, member.getRole());
        assessmentMemberManageAccessService.requireCanTouchRole(actor, newRole);

        member.setRole(newRole);
        return AssessmentMemberDto.toDto(assessmentMemberRepository.save(member));
    }
}
