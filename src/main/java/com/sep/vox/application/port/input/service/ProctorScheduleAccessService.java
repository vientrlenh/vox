package com.sep.vox.application.port.input.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * "Who may view the proctoring data of this session" -- one source of truth for every read path by session.
 *
 * <p>Split out of {@code ViewMyProctorScheduleCandidatesUseCase} when the second read path appeared
 * (alert history). Same reason as {@link AssessmentRecordingAccessService}: copying the permission-check block
 * for a second user means sooner or later the two copies diverge, and a divergence in who may view proctoring data
 * is the kind of bug nobody sees until it is too late.
 */
@Service
@RequiredArgsConstructor
public class ProctorScheduleAccessService {

    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentRepository assessmentRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    /**
     * Returns the assessment schedule if the logged-in user is allowed to proctor it, throws an error if not.
     *
     * <p>Allowed means: the proctor assigned to this schedule, or the school admin of the school that owns the
     * assessment.
     */
    public AssessmentSchedule requireCanMonitorSchedule(UUID scheduleId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!hasAccess(scheduleId, currentUserId)) {
            throw new ForbiddenException("You are not a proctor of this assessment schedule");
        }
        AssessmentSchedule schedule = assessmentScheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Assessment schedule not found"));
        if (schedule.getStatus() != null && schedule.getStatus().isRemoved()) {
            throw new ResourceNotFoundException("Assessment schedule not found");
        }
        return schedule;
    }

    private boolean hasAccess(UUID scheduleId, UUID currentUserId) {
        if (assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, currentUserId)) {
            return true;
        }

        boolean isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (!isSchoolAdmin) {
            return false;
        }

        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        AssessmentSchedule schedule = assessmentScheduleRepository.findById(scheduleId).orElse(null);
        if (currentSchoolId == null || schedule == null) {
            return false;
        }
        return assessmentRepository.findById(schedule.getAssessmentId())
            .map(assessment -> currentSchoolId.equals(assessment.getSchoolId()))
            .orElse(false);
    }
}
