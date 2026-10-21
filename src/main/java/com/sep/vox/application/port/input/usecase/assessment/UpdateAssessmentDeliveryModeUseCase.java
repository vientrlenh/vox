package com.sep.vox.application.port.input.usecase.assessment;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.UpdateAssessmentDeliveryModeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.assessment.AssessmentDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.service.assessment.AssessmentDeliveryModeSupport;

@Service
public class UpdateAssessmentDeliveryModeUseCase implements IUseCase<UpdateAssessmentDeliveryModeCommand, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public UpdateAssessmentDeliveryModeUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentPaperRepository assessmentPaperRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentDto execute(UpdateAssessmentDeliveryModeCommand input) {
        var deliveryMode = AssessmentDeliveryModeSupport.parse(input.deliveryMode());

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        authorize(assessment, currentUserId, currentSchoolId, schoolAdmin);

        if (assessment.getStatus() != AssessmentStatus.DRAFT && assessment.getStatus() != AssessmentStatus.SCHEDULED) {
            throw new IllegalStateException("Không thể đổi hình thức làm bài khi bài kiểm tra đã bắt đầu hoặc kết thúc");
        }
        if (deliveryMode != assessment.getDeliveryMode()
                && assessmentCandidateRepository.existsByAssessmentIdAndScheduleIdIsNotNull(assessment.getId())) {
            throw new IllegalStateException(
                "Không thể đổi hình thức làm bài khi đã có thí sinh được xếp vào ca thi — hãy gỡ hết thí sinh khỏi ca thi trước");
        }

        assessment.setDeliveryMode(deliveryMode);
        assessment.setUpdatedAt(Instant.now());
        assessment.setUpdatedBy(currentUserId);

        var saved = assessmentRepository.save(assessment);
        return AssessmentDto.toDto(saved, papersLocked(saved.getId()));
    }

    private void authorize(Assessment assessment, UUID currentUserId, UUID currentSchoolId, boolean schoolAdmin) {
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId())) {
            return;
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            return;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }

    private boolean papersLocked(UUID assessmentId) {
        var papers = assessmentPaperRepository.findByAssessmentId(assessmentId);
        return !papers.isEmpty()
            && papers.stream().allMatch(paper -> paper.getStatus() == AssessmentPaperStatus.LOCKED);
    }
}
