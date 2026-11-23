package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.Assessment;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateAssessmentMemberCommand;
import com.sep.vox.application.port.input.service.AssessmentMemberManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.AssessmentMemberDto;
import com.sep.vox.domain.mapper.AssessmentMemberDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class CreateAssessmentMemberUseCase implements IUseCase<CreateAssessmentMemberCommand, AssessmentMemberDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentMemberManageAccessService assessmentMemberManageAccessService;

    @Override
    @Transactional
    public AssessmentMemberDto execute(CreateAssessmentMemberCommand input) {
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        AssessmentMemberManageAccessService.AssessmentMemberActor actor = assessmentMemberManageAccessService.requireCanManage(assessment);
        AssessmentMemberRole role = input.role();
        assessmentMemberManageAccessService.requireCanTouchRole(actor, role);

        if (!schoolUserRepository.existsBySchoolIdAndUserId(assessment.getSchoolId(), input.userId())) {
            throw new IllegalStateException("Người dùng không thuộc trường của bài kiểm tra");
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), input.userId(), role)) {
            throw new IllegalStateException("Người dùng đã có vai trò này trong bài kiểm tra");
        }

        AssessmentMember member = AssessmentMember.builder()
                .assessmentId(assessment.getId())
                .userId(input.userId())
                .role(role)
                .grantedAt(Instant.now())
                .grantedBy(actor.userId())
                .build();
        return AssessmentMemberDtoMapper.toDto(assessmentMemberRepository.save(member));
    }
}
