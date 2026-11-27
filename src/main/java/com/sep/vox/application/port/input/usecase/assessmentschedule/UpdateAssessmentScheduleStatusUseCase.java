package com.sep.vox.application.port.input.usecase.assessmentschedule;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentschedule.UpdateAssessmentScheduleStatusCommand;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.mapper.AssessmentScheduleDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentScheduleStatusUseCase implements IUseCase<UpdateAssessmentScheduleStatusCommand, AssessmentScheduleDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentScheduleProctorConflictValidator assessmentScheduleProctorConflictValidator;
    private final AssessmentScheduleCandidateConflictValidator assessmentScheduleCandidateConflictValidator;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentScheduleDto execute(UpdateAssessmentScheduleStatusCommand input) {
        AssessmentSchedule schedule = assessmentScheduleRepository.findById(input.scheduleId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment schedule not found"));
        if (!schedule.getAssessmentId().equals(input.assessmentId())) {
            throw new ResourceNotFoundException("Assessment schedule not found");
        }
        Assessment assessment = assessmentRepository.findById(schedule.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        UUID currentUserId = authorize(assessment);

        applyAction(input, schedule, assessment, currentUserId);

        return AssessmentScheduleDtoMapper.toDto(assessmentScheduleRepository.save(schedule));
    }

    private void applyAction(
            UpdateAssessmentScheduleStatusCommand input,
            AssessmentSchedule schedule,
            Assessment assessment,
            UUID currentUserId) {
        Instant now = Instant.now();
        String action = input.action() == null ? "" : input.action().trim().toUpperCase();
        switch (action) {
            case "PUBLISH" -> publish(schedule);
            // Moving a session is a scheduling operation, so it is locked once the assessment has started; publish/complete/cancel
            // are operations run while the exam is going on, so they must still work.
            case "MOVE" -> {
                requireAssessmentNotStarted(assessment);
                move(schedule, input.targetScheduleId(), currentUserId, now);
            }
            case "COMPLETE" -> complete(schedule);
            case "CANCEL" -> cancel(schedule);
            default -> throw new IllegalArgumentException("Invalid action");
        }
    }

    private void requireAssessmentNotStarted(Assessment assessment) {
        if (assessment.isLockedForEditing()) {
            throw new IllegalArgumentException("The schedule cannot be changed after the assessment has started");
        }
    }

    private void publish(AssessmentSchedule schedule) {
        if (schedule.getStatus() != AssessmentScheduleStatus.DRAFT) {
            throw new IllegalArgumentException("An assessment schedule can only be published while it is a draft");
        }
        if (assessmentScheduleProctorRepository.countByScheduleId(schedule.getId()) < 1) {
            throw new IllegalArgumentException("At least 1 proctor is required before an assessment schedule is published");
        }
        requireEveryCandidateHasPaper(schedule);
        schedule.setStatus(AssessmentScheduleStatus.PUBLISHED);
    }

    /**
     * A published session is one that students and proctors see and will really enter, so it has to be locked down right
     * here: a missing paper would only blow up when entering the room ({@code VerifyAssessmentScheduleOtpUseCase}). A candidate
     * who is exempted or cancelled does not enter the room so needs no paper, and does not make the session "occupied" either --
     * sharing the classification with {@link AssessmentCandidateStatus}.
     */
    private void requireEveryCandidateHasPaper(AssessmentSchedule schedule) {
        List<AssessmentCandidate> candidates = assessmentCandidateRepository.findByScheduleId(schedule.getId()).stream()
            .filter(candidate -> !AssessmentCandidateStatus.isNonScorable(candidate.getStatus()))
            .toList();
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("The assessment schedule has no candidate, so it cannot be published");
        }
        long withoutPaper = candidates.stream()
            .filter(candidate -> candidate.getAssignedPaperId() == null)
            .count();
        if (withoutPaper > 0) {
            throw new IllegalArgumentException(
                "There are still " + withoutPaper + " students not assigned a paper, so the assessment schedule cannot be published");
        }
    }

    /**
     * Moving a session: the source session must become completely empty, with every candidate and proctor transferred to the
     * target session. If it were only marked MOVED without moving the people, the target session would be empty while the source session
     * still held them -- and the attendance screen would still join out the source session through assessment_schedule_proctors.
     * Room capacity is not checked because school_rooms has no capacity column.
     */
    private void move(AssessmentSchedule schedule, UUID targetScheduleId, UUID currentUserId, Instant now) {
        AssessmentSchedule target = requireMoveTarget(schedule, targetScheduleId);

        moveCandidates(schedule.getId(), target, currentUserId, now);
        moveProctors(schedule.getId(), target);

        schedule.setStatus(AssessmentScheduleStatus.MOVED);
        schedule.setMovedToScheduleId(targetScheduleId);
    }

    private AssessmentSchedule requireMoveTarget(AssessmentSchedule schedule, UUID targetScheduleId) {
        if (schedule.getStatus() != AssessmentScheduleStatus.DRAFT && schedule.getStatus() != AssessmentScheduleStatus.PUBLISHED) {
            throw new IllegalArgumentException("An assessment schedule can only be moved while it is a draft or published");
        }
        if (targetScheduleId == null) {
            throw new IllegalArgumentException("The target assessment schedule is required when moving");
        }
        if (targetScheduleId.equals(schedule.getId())) {
            throw new IllegalArgumentException("The target assessment schedule must differ from the current one");
        }
        // Lock the target session row so it does not run in parallel with placing candidates into that same session.
        AssessmentSchedule target = assessmentScheduleRepository.findByIdForUpdate(targetScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Target assessment schedule not found"));
        if (!target.getAssessmentId().equals(schedule.getAssessmentId())) {
            throw new IllegalArgumentException("The target assessment schedule does not belong to the same assessment");
        }
        if (target.getStatus() != AssessmentScheduleStatus.DRAFT && target.getStatus() != AssessmentScheduleStatus.PUBLISHED) {
            throw new IllegalArgumentException("The target assessment schedule is not in a valid status");
        }
        return target;
    }

    private void moveCandidates(UUID sourceScheduleId, AssessmentSchedule target, UUID currentUserId, Instant now) {
        List<AssessmentCandidate> candidates = assessmentCandidateRepository.findByScheduleId(sourceScheduleId);
        if (candidates.isEmpty()) {
            return;
        }
        // The target schedule has a different time window than the source, so it must be re-checked, symmetric with moveProctors: the candidates
        // that move along may collide with a third schedule of another assessment. The source schedule is excluded (it is the current schedule
        // of every candidate here) and is about to become MOVED. Check the WHOLE batch before touching anyone so
        // nothing is left half-done in memory when the 40th person is the one who conflicts.
        assessmentScheduleCandidateConflictValidator.requireCandidatesFree(
            candidates, target.getStartDate(), target.getEndDate());

        candidates.forEach(candidate -> candidate.assignToSchedule(target.getId(), now, currentUserId));
        assessmentCandidateRepository.saveAll(candidates);
    }

    private void moveProctors(UUID sourceScheduleId, AssessmentSchedule target) {
        for (AssessmentScheduleProctor proctor : assessmentScheduleProctorRepository.findByScheduleId(sourceScheduleId)) {
            // The table has a unique (schedule_id, teacher_id), so a proctor already present in the target schedule is skipped,
            // but the old row must still be deleted so the source schedule is left with no proctor.
            if (!assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(target.getId(), proctor.getTeacherId())) {
                // The target schedule has a different time window than the source, so it must be re-checked: a proctor who moves along may collide
                // with a third schedule. Exclude the source schedule from the check because it is about to become MOVED.
                assessmentScheduleProctorConflictValidator.requireTeacherFree(
                    proctor.getTeacherId(), target.getStartDate(), target.getEndDate(), sourceScheduleId);
                assessmentScheduleProctorRepository.save(
                    AssessmentScheduleProctor.assigned(target.getId(), proctor.getTeacherId()));
            }
            assessmentScheduleProctorRepository.deleteById(proctor.getId());
        }
    }

    private void complete(AssessmentSchedule schedule) {
        if (schedule.getStatus() != AssessmentScheduleStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only a published assessment schedule can be completed");
        }
        schedule.setStatus(AssessmentScheduleStatus.COMPLETED);
    }

    private void cancel(AssessmentSchedule schedule) {
        if (schedule.getStatus() != AssessmentScheduleStatus.DRAFT && schedule.getStatus() != AssessmentScheduleStatus.PUBLISHED) {
            throw new IllegalArgumentException("An assessment schedule can only be cancelled while it is a draft or published");
        }
        schedule.setStatus(AssessmentScheduleStatus.CANCELLED);
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
