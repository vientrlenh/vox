package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.user.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * A student must not be placed in two sessions that overlap in time -- a real person cannot sit in two
 * rooms at once. Parallel to {@link AssessmentScheduleProctorConflictValidator}, only the axis changes from proctor
 * to candidate.
 *
 * <p>The scope of the check is the WHOLE SCHOOL, every assessment: checking only within the same assessment would still let a school that runs
 * several assessments in parallel create overlaps -- which is exactly this bug.
 *
 * <p>Gathered here because six flows all need exactly one rule, and missing any one of them leaves a loophole: placing
 * candidates one by one, bulk placement, automatic even spreading, editing the time of a session that already has candidates, moving a session (candidates follow
 * to the target session with a different time window), and editing the open/close time of a class test (the time is written
 * straight down to the session).
 *
 * <h2>Why {@code excludeScheduleId} does not need to be passed</h2>
 *
 * <p>The constraint {@code unique (assessment_id, student_id)} says each student has at most ONE candidate row
 * per assessment, i.e. at most one session occupied per assessment. So a conflicting row
 * {@code (studentId = S, scheduleId = X)} is the row of candidate {@code C} if and only if
 * {@code X == C.getScheduleId()}: session {@code X} belongs to exactly one assessment, and {@code S} has only one row
 * in that assessment. Filtering by each candidate's own current session therefore excludes EXACTLY their own row,
 * no more and no less -- and automatically yields the exclusion that all six flows need (the session being
 * replaced when rearranging, the source session when moving, the session itself when changing its time).
 */
@Service
@RequiredArgsConstructor
public class AssessmentScheduleCandidateConflictValidator {

    /** Above this count the message names a few representatives and summarizes the rest, so it does not grow without bound. */
    private static final int MAX_NAMES_IN_MESSAGE = 5;

    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final UserRepository userRepository;

    /**
     * Every candidate in {@code candidates} must be free in the window [start, end). Each candidate's own current
     * session is excluded from the check automatically (see the class javadoc).
     */
    public void requireCandidatesFree(Collection<AssessmentCandidate> candidates, Instant start, Instant end) {
        // A session without a time has nothing to compare -- the same way AssessmentScheduleRoomValidator skips when
        // data is missing.
        if (candidates == null || candidates.isEmpty() || start == null || end == null) {
            return;
        }

        // A candidate who is exempted or cancelled does not enter the room, so they do not take a seat, and therefore
        // cannot be placed in a conflict either -- shares the classification with AssessmentCandidateStatus.
        List<AssessmentCandidate> occupying = candidates.stream()
            .filter(candidate -> !AssessmentCandidateStatus.isNonScorable(candidate.getStatus()))
            .toList();
        if (occupying.isEmpty()) {
            return;
        }

        List<UUID> conflictingStudentIds = findConflictingStudentIds(occupying, start, end);
        if (conflictingStudentIds.isEmpty()) {
            return;
        }
        throw new ResourceDuplicatedException(buildMessage(conflictingStudentIds));
    }

    private List<UUID> findConflictingStudentIds(List<AssessmentCandidate> occupying, Instant start, Instant end) {
        // The values are nullable, so HashMap.put is required; Collectors.toMap throws an NPE when a value is null.
        Map<UUID, UUID> currentScheduleByStudent = new HashMap<UUID, UUID>();
        for (AssessmentCandidate candidate : occupying) {
            currentScheduleByStudent.put(candidate.getStudentId(), candidate.getScheduleId());
        }

        return assessmentCandidateRepository
            .findConflictsForStudents(currentScheduleByStudent.keySet(), start, end, null).stream()
            .filter(conflict -> !Objects.equals(
                conflict.scheduleId(), currentScheduleByStudent.get(conflict.studentId())))
            .map(conflict -> conflict.studentId())
            .distinct()
            .toList();
    }

    /**
     * Changing the time of an assessment schedule: every candidate assigned to that schedule must still be free in the new time window.
     *
     * <p>Without this step the rule is easy to bypass — assign candidates while two schedules do not clash yet, then
     * move the time so they overlap.
     */
    public void requireCandidatesFreeForNewWindow(UUID scheduleId, Instant start, Instant end) {
        requireCandidatesFree(assessmentCandidateRepository.findByScheduleId(scheduleId), start, end);
    }

    /**
     * The message names people so the scheduler knows whom to remove, instead of probing the whole list by hand.
     *
     * <p>No time included (no message in the system carries a time zone) and no name of the clashing
     * assessment — the scheduler of this assessment may not have permission to see the other one. The details live in the API
     * {@code studentBusySlots} for the screen.
     */
    private String buildMessage(List<UUID> conflictingStudentIds) {
        List<String> names = resolveNames(conflictingStudentIds);
        if (names.size() == 1) {
            return "Student " + names.getFirst() + " already has another assessment schedule in this time range";
        }
        List<String> shown = names.stream().limit(MAX_NAMES_IN_MESSAGE).toList();
        StringBuilder message = new StringBuilder()
            .append(names.size())
            .append(" students already have another assessment schedule in this time range: ")
            .append(String.join(", ", shown));
        if (names.size() > shown.size()) {
            message.append(" and ").append(names.size() - shown.size()).append(" other students");
        }
        return message.toString();
    }

    /**
     * Names are only looked up once a conflict is certain, so the success path costs no extra query. Without a name
     * we fall back to the id: building the message must never throw and hide the real error.
     */
    private List<String> resolveNames(List<UUID> studentIds) {
        Map<UUID, String> nameById = new HashMap<UUID, String>();
        for (User user : userRepository.findByIdIn(studentIds)) {
            if (user.getFullName() != null && user.getFullName().value() != null) {
                nameById.put(user.getId(), user.getFullName().value());
            }
        }
        List<String> names = new ArrayList<String>(studentIds.size());
        for (UUID studentId : studentIds) {
            names.add(nameById.getOrDefault(studentId, studentId.toString()));
        }
        return names;
    }
}
