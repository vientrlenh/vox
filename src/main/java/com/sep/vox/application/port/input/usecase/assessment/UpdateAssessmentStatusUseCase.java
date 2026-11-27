package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.event.AssessmentResultsPublishedEvent;
import com.sep.vox.application.port.input.command.assessment.UpdateAssessmentStatusCommand;
import com.sep.vox.application.port.input.service.AssessmentCandidateResultFinalizationService;
import com.sep.vox.application.port.input.service.AssessmentScheduleClosureService;
import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.AssessmentHumanGradingNotificationService;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.ZeroScoreAssessmentResultService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.service.assessment.AssessmentScheduleWindowMessages;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentStatusUseCase implements IUseCase<UpdateAssessmentStatusCommand, AssessmentDto> {

    /**
     * Cancellable while the results are not yet finalized. RESULTS_PUBLISHED cannot be withdrawn (students have already seen their scores),
     * and CANCELLED cannot be cancelled again.
     */
    private static final Set<AssessmentStatus> CANCELLABLE_FROM =
        EnumSet.of(AssessmentStatus.DRAFT, AssessmentStatus.SCHEDULED, AssessmentStatus.IN_PROGRESS, AssessmentStatus.CLOSED);

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final AssessmentCandidateResultFinalizationService assessmentCandidateResultFinalizationService;
    private final ZeroScoreAssessmentResultService zeroScoreAssessmentResultService;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserContextPort userContextPort;
    private final EventPublisherPort eventPublisherPort;
    private final ClassTestGradingAssignmentService classTestGradingAssignmentService;
    private final AssessmentHumanGradingNotificationService assessmentHumanGradingNotificationService;
    private final ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;
    private final AssessmentScheduleClosureService assessmentScheduleClosureService;

    @Override
    @Transactional
    public AssessmentDto execute(UpdateAssessmentStatusCommand input) {
        UpdateAssessmentStatusCommand command = new UpdateAssessmentStatusCommand(
            input.assessmentId(),
            StringNormalization.normalizeCode(input.action()),
            StringNormalization.trimAndCollapseSpaces(input.note())
        );
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(user -> user.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        Assessment assessment = assessmentRepository.findById(command.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        authorizeMutation(assessment.getId(), assessment.getSchoolId(), assessment.getKind(), currentUserId, currentSchoolId, schoolAdmin);

        applyAction(assessment, command.action());

        return AssessmentDtoMapper.toDto(assessmentRepository.save(assessment));
    }

    private void applyAction(Assessment assessment, String action) {
        switch (action) {
            case "SCHEDULE" -> applySchedule(assessment);
            case "START" -> applyStart(assessment);
            case "CLOSE" -> applyClose(assessment);
            case "PUBLISH_RESULTS" -> applyPublishResults(assessment);
            case "CANCEL" -> applyCancel(assessment);
            default -> throw new IllegalArgumentException("Invalid action");
        }
    }

    private void applySchedule(Assessment assessment) {
        // The status is checked BEFORE the plan limit: pressing SCHEDULE on an assessment that is no longer DRAFT and getting
        // "exceeds the plan limit" would send the user to fix the wrong thing.
        requireTransition(assessment, AssessmentStatus.DRAFT, AssessmentStatus.SCHEDULED);
        validatePlanLimits(assessment);
        if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
            requireClassTestScheduleReady(assessment);
        } else {
            requireCentralizedScheduleReadiness(assessment);
        }
    }

    private void applyStart(Assessment assessment) {
        if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
            requireClassTestCanStart(assessment);
        }
        requireTransition(assessment, AssessmentStatus.SCHEDULED, AssessmentStatus.IN_PROGRESS);
        if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
            lockClassTestPapers(assessment.getId());
        }
    }

    private void applyClose(Assessment assessment) {
        Instant now = Instant.now();
        requireTransition(assessment, AssessmentStatus.IN_PROGRESS, AssessmentStatus.CLOSED);
        // Check the status first and only then touch the sessions, same as the CANCEL branch.
        assessmentScheduleClosureService.requireNoActiveSessionInOngoingSchedule(assessment.getId(), now);
        assessmentScheduleClosureService.closeSchedulesForAssessment(assessment.getId(), now);
        assessmentQuestionSecureLockService.releaseIfAutoAfterClose(assessment.getId());
        zeroScoreAssessmentResultService.ensureZeroResultsForMissingOrEmptyAttempts(assessment.getId());
        // The catch-up sweep runs AFTER the zero results have been filled in: a class test has nobody coordinating grading,
        // so closing the assessment is the last chance for the owning teacher to receive everything that remains.
        classTestGradingAssignmentService.ensureAssignmentsForAssessment(assessment.getId());
        // After the sweep: the "how many are still waiting for grading" number is only right once the queue is complete.
        // Applies to BOTH kinds -- the sweep above only covers class tests.
        assessmentHumanGradingNotificationService.publishIfPendingReview(assessment, now);
    }

    private void applyPublishResults(Assessment assessment) {
        zeroScoreAssessmentResultService.ensureZeroResultsForMissingOrEmptyAttempts(assessment.getId());
        requirePublishReadiness(assessment.getId());
        requireTransition(assessment, AssessmentStatus.CLOSED, AssessmentStatus.RESULTS_PUBLISHED);
        finalizePassFailForAssessment(assessment);
        eventPublisherPort.publish(new AssessmentResultsPublishedEvent(assessment.getId()));
    }

    private void applyCancel(Assessment assessment) {
        // Check the status first and only then touch the sessions: an invalid cancel must not write anything.
        requireTransition(assessment, CANCELLABLE_FROM, AssessmentStatus.CANCELLED);
        assessmentScheduleClosureService.cancelSchedulesForAssessment(assessment.getId());
    }

    private void authorizeMutation(
            UUID assessmentId,
            UUID assessmentSchoolId,
            AssessmentKind kind,
            UUID currentUserId,
            UUID currentSchoolId,
            boolean schoolAdmin) {
        if (kind == AssessmentKind.CENTRALIZED) {
            if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessmentSchoolId)) {
                return;
            }
            // The council chair runs the whole centralized assessment workflow on the detail page, so they are also the
            // one who presses schedule / start / close / publish results. Deleting an assessment is still only for the school
            // administrator (see DeleteAssessmentUseCase) -- that is a destructive action and is not part of this workflow.
            if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR)) {
                return;
            }
            throw new ForbiddenException("Access denied");
        }
        if (!assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR)) {
            throw new ForbiddenException("Access denied");
        }
    }

    private void requireTransition(Assessment assessment, AssessmentStatus from, AssessmentStatus to) {
        requireTransition(assessment, EnumSet.of(from), to);
    }

    /**
     * Variant with several source statuses, currently used only by CANCEL: an assessment can be cancelled from any
     * status that is not final, but published results cannot be withdrawn and a cancelled assessment cannot be
     * cancelled again.
     */
    private void requireTransition(Assessment assessment, Set<AssessmentStatus> allowedFrom, AssessmentStatus to) {
        if (!allowedFrom.contains(assessment.getStatus())) {
            throw new IllegalArgumentException("The current assessment status is not valid for this action");
        }
        assessment.setStatus(to);
    }

    private void validatePlanLimits(Assessment assessment) {
        SchoolSubscription activeSubscription = schoolSubscriptionRepository.findActiveBySchoolId(assessment.getSchoolId())
            .orElseThrow(() -> new BusinessRuleViolationException(
                "The school has no active subscription plan, so the assessment cannot be scheduled"));
        SubscriptionPlan plan = subscriptionPlanRepository.findById(activeSubscription.getSubscriptionPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found"));

        if (assessment.getAssessmentTimeDurationSecond() != null && plan.getMaxTimePerAttemptMin() != null
                && assessment.getAssessmentTimeDurationSecond() > plan.getMaxTimePerAttemptMin() * 60) {
            throw new BusinessRuleViolationException(
                "The assessment duration (" + assessment.getAssessmentTimeDurationSecond() + " seconds) exceeds the limit of the plan \""
                    + plan.getName() + "\" (at most " + plan.getMaxTimePerAttemptMin() + " minutes per attempt)"
            );
        }

        // Worst-case estimate: every candidate uses the whole assessment duration on every attempt. Check the GRADING
        // quota first (and, for a class test, also CLASS_TEST plus the teacher's personal quota) -- see
        // ClassTestTokenQuotaGuardService for why all 3 places must be checked.
        classTestTokenQuotaGuardService.requireWithinTokenQuota(assessment);
    }

    /**
     * A class test is also taken in a room with proctors, like a centralized assessment, so before the schedule is
     * published it needs: assessment schedules with a room and a proctor, and every student placed in a schedule
     * and assigned a paper.
     *
     * <p>Choosing a class at creation time only loads students into the candidate list; placing them in schedules is
     * a separate step, so without this block the test could still be scheduled with a whole class that has no
     * schedule and nobody could enter the assessment.
     */
    private void requireClassTestScheduleReadiness(
            Assessment assessment,
            List<AssessmentSchedule> schedules) {
        requireSchedulesHaveRoomAndProctor(schedules);

        List<AssessmentCandidate> candidates = assessmentCandidateRepository.findByAssessmentId(assessment.getId());
        long withoutSchedule = candidates.stream()
            .filter(candidate -> candidate.getScheduleId() == null)
            .count();
        if (withoutSchedule > 0) {
            throw new IllegalArgumentException(
                "There are still " + withoutSchedule + " students not placed in an assessment schedule");
        }
        long withoutPaper = candidates.stream()
            .filter(candidate -> candidate.getAssignedPaperId() == null)
            .count();
        if (withoutPaper > 0) {
            throw new IllegalArgumentException("There are still " + withoutPaper + " students not assigned a paper");
        }
    }

    /**
     * The sessions of the assessment that are still in effect. A cancelled/moved session will never take place so it must not take part in
     * any scheduling condition: if their room/proctors were checked the assessment would be blocked
     * by an abandoned session, and if they were counted in "has a session" the assessment could be scheduled with no session
     * actually running. {@code findByAssessmentId} only filters out DELETED beforehand so the extra filtering has to happen here.
     */
    private List<AssessmentSchedule> effectiveSchedules(UUID assessmentId) {
        return assessmentScheduleRepository.findByAssessmentId(assessmentId).stream()
            .filter(schedule -> schedule.getStatus() != null && schedule.getStatus().isInEffect())
            .toList();
    }

    /** Every assessment schedule still in force needs a room and at least one proctor -- true for both kinds of assessment. */
    private void requireSchedulesHaveRoomAndProctor(
            List<AssessmentSchedule> schedules) {
        for (AssessmentSchedule schedule : schedules) {
            if (schedule.getSchoolRoomId() == null) {
                throw new IllegalArgumentException("The assessment schedule has no room selected");
            }
            if (assessmentScheduleProctorRepository.countByScheduleId(schedule.getId()) == 0) {
                throw new IllegalArgumentException("The assessment schedule has no proctor");
            }
        }
    }

    /**
     * A centralized assessment used to be schedulable even with no session, no candidate and no paper
     * variant yet -- the assessment carried the SCHEDULED status although in practice nobody could take it.
     *
     * <p>The check here is one notch lighter than for a class test: it does NOT require each candidate to already have a session and a paper, because
     * that rule already lives in the step that publishes each session ({@code UpdateAssessmentScheduleStatusUseCase.publish}: a session must
     * have candidates and every candidate in it must have a paper). A centralized assessment therefore has to place candidates into sessions
     * ({@code AssignAssessmentCandidateSchedule}/{@code AutoFill}) and assign papers ({@code AssignAssessmentPapersUseCase},
     * which requires every paper to be LOCKED) BEFORE publishing the sessions -- both operations can run while the assessment is still DRAFT.
     * Here only the "empty" case that has no way of running is blocked.
     *
     * <p>In addition every session must already be published: a session still in DRAFT is not yet visible to students and proctors,
     * and an assessment that is "scheduled" with a session nobody can enter would be wrong.
     */
    private void requireCentralizedScheduleReadiness(Assessment assessment) {
        List<AssessmentSchedule> schedules = effectiveSchedules(assessment.getId());
        if (schedules.isEmpty()) {
            throw new IllegalArgumentException("The assessment has no assessment schedule, so it cannot be scheduled");
        }
        requireSchedulesHaveRoomAndProctor(schedules);
        if (assessmentCandidateRepository.countByAssessmentId(assessment.getId()) == 0) {
            throw new IllegalArgumentException("The assessment has no candidate, so it cannot be scheduled");
        }
        if (assessmentPaperRepository.findByAssessmentId(assessment.getId()).isEmpty()) {
            throw new IllegalArgumentException("The assessment has no paper, so it cannot be scheduled");
        }
        // Publishing each session is a separate operation (UpdateAssessmentScheduleStatusUseCase) and does not require the status of the
        // assessment, so blocking here does not lock the flow: publish all the sessions first, then the assessment can be scheduled.
        long draftScheduleCount = schedules.stream()
            .filter(schedule -> schedule.getStatus() == AssessmentScheduleStatus.DRAFT)
            .count();
        if (draftScheduleCount > 0) {
            throw new IllegalArgumentException(
                "There are still " + draftScheduleCount + " assessment schedules not published, so the assessment cannot be scheduled");
        }
    }

    /**
     * This function used to push every DRAFT session to PUBLISHED by itself when the teacher pressed schedule -- so a class test and a
     * centralized assessment ran two opposite models, and the session changed status behind the user's back. Now it only CHECKS, it
     * does not write: publishing each session is a separate operation
     * ({@code UpdateAssessmentScheduleStatusUseCase}, regardless of the assessment kind), and scheduling the assessment only locks in
     * the outcome of that operation -- exactly like {@link #requireCentralizedScheduleReadiness}.
     */
    private void requireClassTestScheduleReady(Assessment assessment) {
        requireClassTestScheduleWindow(assessment);
        List<AssessmentSchedule> schedules = effectiveSchedules(assessment.getId());
        if (schedules.isEmpty()) {
            throw new IllegalArgumentException("The class test has no schedule");
        }
        requireClassTestScheduleReadiness(assessment, schedules);
        long draftScheduleCount = schedules.stream()
            .filter(schedule -> schedule.getStatus() == AssessmentScheduleStatus.DRAFT)
            .count();
        if (draftScheduleCount > 0) {
            throw new IllegalArgumentException(
                "There are still " + draftScheduleCount + " assessment schedules not published, so the class test cannot be scheduled");
        }
    }

    private void requireClassTestScheduleWindow(Assessment assessment) {
        if (assessment.getOpenAt() == null || assessment.getCloseAt() == null) {
            throw new IllegalArgumentException("A class test must have an opening time and a closing time before it is scheduled");
        }
        if (!assessment.getOpenAt().isBefore(assessment.getCloseAt())) {
            throw new IllegalArgumentException("The opening time must be earlier than the closing time");
        }
        // The session of a class test follows exactly the open/close window, so the window has to be enough for the attempt time.
        // Placed here (not in CreateClassTestUseCase.createDraftSchedule) because when the session is created
        // assessmentTimeDurationSecond is still the raw value the user typed, not yet passed through recalculate.
        if (assessment.isScheduleWindowShorterThanAssessmentTime(assessment.getOpenAt(), assessment.getCloseAt())) {
            throw new IllegalArgumentException(AssessmentScheduleWindowMessages.schedulesNoLongerFit(1, assessment));
        }
    }

    private void requireClassTestCanStart(Assessment assessment) {
        Instant now = Instant.now();
        if (assessment.getCloseAt() != null && !now.isBefore(assessment.getCloseAt())) {
            throw new IllegalArgumentException("The class test closing time has already passed");
        }
    }

    /**
     * G.3: only allow publish when EVERY AssessmentCandidateResult of the assessment is exactly RELEASED
     * or INVALID (the 2 states "processed, ready to be finalized") - any other
     * status (PENDING_REVIEW not yet reviewed, FINAL/APPEALED/RE_GRADING/RETAKE_REQUIRED from the
     * appeal/suspicion flow) blocks publish until it is fully handled.
     *
     * <p>SOFT-DELETED SESSIONS/RESULTS (status DELETED) MUST BE EXCLUDED FROM BOTH LOOPS BELOW.
     * A soft delete does not remove the row, it only changes status -- {@code findByCandidateId}/{@code findByAssessmentId}
     * do not filter by status on their own, so without an explicit exclusion a student who failed the attempt, had the session
     * deleted and then retook it (DeleteAssessmentSessionUseCase) would block publishing OF THE WHOLE ASSESSMENT
     * forever: DELETED never turns into RELEASED/INVALID by itself (AssessmentCandidateResultFinalizationService.
     * finalizeForPublish also deliberately skips DELETED, see "out of scope, kept as is" there),
     * and a deleted session obviously has no NEW result to fill in. Measured for real: blocked on exactly
     * the line "N results are still not in RELEASED or INVALID" even though the assessment has no result
     * really hanging -- the replacement session was graded, only the deleted OLD session stands still.
     */
    private void requirePublishReadiness(UUID assessmentId) {
        requireEverySessionHasResult(assessmentId);
        requireEveryResultReady(assessmentId);
    }

    private void requireEverySessionHasResult(UUID assessmentId) {
        long missingResultCount = assessmentCandidateRepository.findByAssessmentId(assessmentId).stream()
            .filter(candidate -> !AssessmentCandidateStatus.isNonScorable(candidate.getStatus()))
            .flatMap(candidate -> assessmentSessionRepository.findByCandidateId(candidate.getId()).stream())
            .filter(session -> assessmentId.equals(session.getAssessmentId()))
            .filter(session -> session.getStatus() != AssessmentSessionStatus.IN_PROGRESS
                && session.getStatus() != AssessmentSessionStatus.INTERRUPTED
                // A deleted session never "has a result" (its result was deleted along with it,
                // see DeleteAssessmentSessionUseCase), but it is NOT what is blocking publish -- it is
                // skipped on purpose, not missed.
                && session.getStatus() != AssessmentSessionStatus.DELETED)
            .filter(session -> assessmentCandidateResultRepository.findBySessionId(session.getId()).isEmpty())
            .count();
        if (missingResultCount > 0) {
            throw new IllegalArgumentException(
                "There are still " + missingResultCount + " assessment sessions without a result, so the results cannot be published");
        }
    }

    private void requireEveryResultReady(UUID assessmentId) {
        long notReadyCount = assessmentCandidateResultRepository.findByAssessmentId(assessmentId).stream()
            .filter(result -> result.getStatus() != AssessmentCandidateResultStatus.RELEASED
                && result.getStatus() != AssessmentCandidateResultStatus.INVALID
                && result.getStatus() != AssessmentCandidateResultStatus.DELETED)
            .count();
        if (notReadyCount > 0) {
            throw new IllegalArgumentException(
                "There are still " + notReadyCount + " results that are neither RELEASED nor INVALID, so the results cannot be published");
        }
    }

    /**
     * G.3: finalizes the result of every AssessmentCandidateResult of the assessment right when it has just moved to
     * RESULTS_PUBLISHED - INVALID -> FAILED (score forced to 0), RELEASED/FINAL -> PASSED/FAILED
     * by passingScore if the policy has a threshold, or -> FINAL if there is no threshold (the school
     * chooses PASSED/FAILED later through decideAssessmentCandidateResultOutcome). See
     * AssessmentCandidateResultFinalizationService.finalizeForPublish.
     */
    private void finalizePassFailForAssessment(Assessment assessment) {
        BigDecimal passingScore = assessment.getAssessmentPolicyId() == null
            ? null
            : assessmentPolicyRepository.findById(assessment.getAssessmentPolicyId())
                .map(policy -> policy.getPassingScore())
                .orElse(null);

        for (AssessmentCandidateResult result : assessmentCandidateResultRepository.findByAssessmentId(assessment.getId())) {
            if (assessmentCandidateResultFinalizationService.finalizeForPublish(result, passingScore)) {
                assessmentCandidateResultRepository.save(result);
            }
        }
    }

    private void lockClassTestPapers(UUID assessmentId) {
        for (AssessmentPaper paper : assessmentPaperRepository.findByAssessmentId(assessmentId)) {
            if (paper.getStatus() != AssessmentPaperStatus.LOCKED) {
                paper.setStatus(AssessmentPaperStatus.LOCKED);
                assessmentPaperRepository.save(paper);
            }
        }
    }
}
