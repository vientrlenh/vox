package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateAssessmentDeliveryModeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

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
        AssessmentDeliveryMode deliveryMode = input.deliveryMode();

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

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

        Assessment saved = assessmentRepository.save(assessment);
        return AssessmentDtoMapper.toDto(saved, papersLocked(saved.getId()));
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
        List<AssessmentPaper> papers = assessmentPaperRepository.findByAssessmentId(assessmentId);
        return !papers.isEmpty()
            && papers.stream().allMatch(paper -> paper.getStatus() == AssessmentPaperStatus.LOCKED);
    }
}
