package com.sep.vox.application.port.input.usecase.assessmentschedule;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteAssessmentScheduleCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class DeleteAssessmentScheduleUseCase implements IUseCase<DeleteAssessmentScheduleCommand, UUID> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public DeleteAssessmentScheduleUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentScheduleProctorRepository assessmentScheduleProctorRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentScheduleProctorRepository = assessmentScheduleProctorRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(DeleteAssessmentScheduleCommand input) {
        AssessmentSchedule schedule = assessmentScheduleRepository.findById(input.scheduleId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
        if (!schedule.getAssessmentId().equals(input.assessmentId())) {
            throw new NotFoundException("Không tìm thấy ca thi");
        }
        Assessment assessment = assessmentRepository.findById(schedule.getAssessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        UUID currentUserId = authorize(assessment);

        if (assessmentCandidateRepository.countByScheduleId(schedule.getId()) > 0) {
            throw new IllegalStateException("Không thể xoá ca thi đang có thí sinh");
        }
        // The session is soft-deleted but no FK/cascade cleans up the assessment_schedule_proctors rows, so the
        // proctors must be removed first -- otherwise the attendance screen still joins out the deleted session.
        if (assessmentScheduleProctorRepository.countByScheduleId(schedule.getId()) > 0) {
            throw new IllegalStateException("Không thể xoá ca thi đang có giám thị");
        }
        if (assessment.isLockedForEditing()) {
            throw new IllegalStateException("Không thể thay đổi lịch thi khi kỳ thi đã bắt đầu");
        }

        schedule.setStatus(AssessmentScheduleStatus.DELETED);
        assessmentScheduleRepository.save(schedule);
        return schedule.getId();
    }

    private UUID authorize(Assessment assessment) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId())) {
            return currentUserId;
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            return currentUserId;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
