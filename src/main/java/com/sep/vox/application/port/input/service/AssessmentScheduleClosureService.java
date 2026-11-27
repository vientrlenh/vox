package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * The ONLY place that states the rule "close the sessions according to the assessment".
 *
 * <p>Previously closing an assessment ({@code CLOSE}) did not touch its sessions, so a closed assessment left its sessions in
 * PUBLISHED forever: such a session passed both {@code isVisibleToStudent()} and {@code allowsAttendance()},
 * and COMPLETED could only be reached when someone clicked each session by hand. The most common way to close an assessment
 * is {@code AssessmentStatusAutoTransitionJob} -- where there is nobody to click.
 *
 * <p>Three paths call in here: closing an assessment ({@code UpdateAssessmentStatusUseCase} + the job), cancelling/deleting one
 * ({@code UpdateAssessmentStatusUseCase} + {@code DeleteAssessmentUseCase} -- previously two hand-copied versions of
 * the same function, with a note "change one, change both"), and the job that sweeps sessions whose time has run out.
 */
@Service
@RequiredArgsConstructor
public class AssessmentScheduleClosureService {

    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;

    /**
     * Blocks closing an assessment while a running session still has people in the middle of an attempt.
     *
     * <p>Deliberately does NOT block strictly by time window: a class test has exactly one session that coincides exactly with
     * openAt-closeAt, so blocking by time would mean the teacher can never close early, and the only remaining
     * way out (cancelling the session) makes {@code allowsAttendance()} return false => the right to fix the attendance of
     * a session that really took place is lost.
     *
     * <p>Closing an assessment midway does not lose data -- {@code DeferredAssessmentSessionGradingJob} still forces
     * submission and grades the part that was done. This guard exists to prevent an accidental click that cuts a session short, so when it blocks,
     * the right way out is {@code forceEndAssessmentSession} for exactly the session that is still hanging.
     */
    public void requireNoActiveSessionInOngoingSchedule(UUID assessmentId, Instant now) {
        if (assessmentScheduleRepository.findByAssessmentIdAndInSchedule(assessmentId, now).isEmpty()) {
            return;
        }
        long activeCount = assessmentSessionRepository.countActiveByAssessmentId(assessmentId);
        if (activeCount > 0) {
            throw new IllegalArgumentException("There are still " + activeCount
                + " students taking the assessment in a running schedule, so the assessment cannot be closed");
        }
    }

    /**
     * Cascade when an assessment is CLOSED: a schedule whose end time has passed → COMPLETED (it really took place), a schedule that
     * never ran (still DRAFT, or published but not yet due) → CANCELLED (it will never take place).
     *
     * <p>The split is made by time instead of using a single status: merging into COMPLETED would label a schedule nobody
     * entered as "completed" (wrong for attendance and reports), while merging into CANCELLED would record a schedule that
     * was already held as "cancelled" (wrong history).
     *
     * @return the number of schedules whose status changed
     */
    public int closeSchedulesForAssessment(UUID assessmentId, Instant now) {
        int changed = 0;
        for (AssessmentSchedule schedule : assessmentScheduleRepository.findByAssessmentId(assessmentId)) {
            if (isTerminal(schedule.getStatus())) {
                continue;
            }
            // Only a PUBLISHED schedule can have really been held: a DRAFT schedule was never visible to
            // students even if its time window has passed -- calling it COMPLETED would invent a session that did not happen.
            // An ongoing schedule cannot reach this point because the guard runs first; if it does, it has really
            // started, so COMPLETED is still a better label than CANCELLED.
            boolean alreadyRan = schedule.getStatus() == AssessmentScheduleStatus.PUBLISHED
                && (schedule.hasEndedAt(now) || schedule.isOngoingAt(now));
            AssessmentScheduleStatus target = alreadyRan ? AssessmentScheduleStatus.COMPLETED : AssessmentScheduleStatus.CANCELLED;
            apply(schedule, target);
            changed++;
        }
        return changed;
    }

    /**
     * Cascade when an assessment is CANCELLED or DELETED: every schedule that is not finished → CANCELLED, whether it has run or
     * not. That is the difference from closing: when the assessment is cancelled its schedules are cancelled too.
     *
     * @return the number of schedules whose status changed
     */
    public int cancelSchedulesForAssessment(UUID assessmentId) {
        int changed = 0;
        for (AssessmentSchedule schedule : assessmentScheduleRepository.findByAssessmentId(assessmentId)) {
            if (isTerminal(schedule.getStatus())) {
                continue;
            }
            apply(schedule, AssessmentScheduleStatus.CANCELLED);
            changed++;
        }
        return changed;
    }

    /**
     * A published schedule whose end time has passed → COMPLETED, at most {@code limit} schedules per run. This is the
     * normal path: a schedule closes itself when time runs out, without waiting for the assessment to close.
     *
     * @return the number of schedules whose status changed
     */
    public int completeEndedSchedules(Instant now, int limit) {
        List<AssessmentSchedule> ended = assessmentScheduleRepository.findPublishedEndedBefore(now, limit);
        for (AssessmentSchedule schedule : ended) {
            apply(schedule, AssessmentScheduleStatus.COMPLETED);
        }
        return ended.size();
    }

    /**
     * COMPLETED/MOVED/CANCELLED are final states: overwriting them would erase the trace of a session that has already been held and would
     * skew {@code movedToScheduleId}. DELETED is already filtered out by {@code findByAssessmentId}. The cascade skips them
     * rather than throwing an error.
     */
    private boolean isTerminal(AssessmentScheduleStatus status) {
        return status != AssessmentScheduleStatus.DRAFT && status != AssessmentScheduleStatus.PUBLISHED;
    }

    private void apply(AssessmentSchedule schedule, AssessmentScheduleStatus target) {
        schedule.setStatus(target);
        assessmentScheduleRepository.save(schedule);
    }
}
