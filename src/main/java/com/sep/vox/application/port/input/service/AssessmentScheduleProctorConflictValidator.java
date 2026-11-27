package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;

/**
 * A proctor must not supervise two sessions that overlap in time -- a real person cannot be present in two
 * rooms at once. Parallel to {@link AssessmentScheduleRoomValidator}, only the axis changes from room to person.
 *
 * <p>The scope of the check is the WHOLE SCHOOL, every assessment: checking only within the same assessment would still let a school that runs
 * several assessments in parallel create overlaps.
 *
 * <p>Gathered here because four flows all need exactly one rule, and missing any one of them leaves a loophole:
 * adding a proctor to a session, editing the time of a session that already has proctors, moving a session (proctors follow to the target
 * session with a different time window), and creating a class test (the creator is automatically assigned as proctor).
 */
@Service
@RequiredArgsConstructor
public class AssessmentScheduleProctorConflictValidator {

    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;

    /**
     * The teacher must be free within [start, end); {@code excludeScheduleId} skips the schedule
     * being operated on.
     */
    public void requireTeacherFree(UUID teacherId, Instant start, Instant end, UUID excludeScheduleId) {
        if (assessmentScheduleProctorRepository.existsOverlappingAssignment(teacherId, start, end, excludeScheduleId)) {
            throw new ResourceDuplicatedException("The proctor already has another assessment schedule in this time range");
        }
    }

    /**
     * Changing the time of an assessment schedule: every proctor on duty in that schedule must still be free in the new time window.
     *
     * <p>Without this step the rule is easy to bypass — assign proctors while two schedules do not clash yet, then
     * move the time so they overlap.
     */
    public void requireProctorsFreeForNewWindow(UUID scheduleId, Instant start, Instant end) {
        for (AssessmentScheduleProctor proctor : assessmentScheduleProctorRepository.findByScheduleId(scheduleId)) {
            requireTeacherFree(proctor.getTeacherId(), start, end, scheduleId);
        }
    }
}
