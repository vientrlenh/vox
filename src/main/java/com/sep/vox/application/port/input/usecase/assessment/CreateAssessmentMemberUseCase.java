package com.sep.vox.application.port.input.usecase.assessment;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.CreateAssessmentMemberCommand;
import com.sep.vox.application.port.input.service.AssessmentMemberManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.dto.AssessmentMemberDto;
import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;

@Service
public class CreateAssessmentMemberUseCase implements IUseCase<CreateAssessmentMemberCommand, AssessmentMemberDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentMemberManageAccessService assessmentMemberManageAccessService;

    public CreateAssessmentMemberUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            SchoolUserRepository schoolUserRepository,
            AssessmentMemberManageAccessService assessmentMemberManageAccessService) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.assessmentMemberManageAccessService = assessmentMemberManageAccessService;
    }

    @Override
    @Transactional
    public AssessmentMemberDto execute(CreateAssessmentMemberCommand input) {
        var command = new CreateAssessmentMemberCommand(
            input.assessmentId(),
            input.userId(),
            StringNormalization.normalizeCode(input.role())
        );
        var assessment = assessmentRepository.findById(command.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        var actor = assessmentMemberManageAccessService.requireCanManage(assessment);
        var role = AssessmentMemberRole.valueOf(command.role());
        assessmentMemberManageAccessService.requireCanTouchRole(actor, role);

        if (!schoolUserRepository.existsBySchoolIdAndUserId(assessment.getSchoolId(), command.userId())) {
            throw new IllegalStateException("Người dùng không thuộc trường của bài kiểm tra");
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), command.userId(), role)) {
            throw new IllegalStateException("Người dùng đã có vai trò này trong bài kiểm tra");
        }

        var member = new AssessmentMember(
            assessment.getId(),
            command.userId(),
            role,
            Instant.now(),
            actor.userId()
        );
        return AssessmentMemberDto.toDto(assessmentMemberRepository.save(member));
    }
}
