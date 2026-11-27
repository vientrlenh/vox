package com.sep.vox.application.port.input.usecase.assessmentschedule;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentschedule.DeleteAssessmentScheduleCommand;
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
@RequiredArgsConstructor
public class DeleteAssessmentScheduleUseCase implements IUseCase<DeleteAssessmentScheduleCommand, UUID> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(DeleteAssessmentScheduleCommand input) {
        AssessmentSchedule schedule = assessmentScheduleRepository.findById(input.scheduleId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment schedule not found"));
        if (!schedule.getAssessmentId().equals(input.assessmentId())) {
            throw new ResourceNotFoundException("Assessment schedule not found");
        }
        Assessment assessment = assessmentRepository.findById(schedule.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        authorize(assessment);

        requireScheduleDeletable(schedule, assessment);

        schedule.setStatus(AssessmentScheduleStatus.DELETED);
        assessmentScheduleRepository.save(schedule);
        return schedule.getId();
    }

    private void requireScheduleDeletable(AssessmentSchedule schedule, Assessment assessment) {
        if (assessmentCandidateRepository.countByScheduleId(schedule.getId()) > 0) {
            throw new IllegalArgumentException("An assessment schedule that has candidates cannot be deleted");
        }
        // The session is soft-deleted but no FK/cascade cleans up the assessment_schedule_proctors rows, so the
        // proctors must be removed first -- otherwise the attendance screen still joins out the deleted session.
        if (assessmentScheduleProctorRepository.countByScheduleId(schedule.getId()) > 0) {
            throw new IllegalArgumentException("An assessment schedule that has proctors cannot be deleted");
        }
        if (assessment.isLockedForEditing()) {
            throw new IllegalArgumentException("The schedule cannot be changed after the assessment has started");
        }
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
        throw new ForbiddenException("Access denied");
    }
}
