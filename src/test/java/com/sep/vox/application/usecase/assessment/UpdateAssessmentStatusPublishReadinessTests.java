package com.sep.vox.application.usecase.assessment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.assessment.UpdateAssessmentStatusCommand;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentStatusUseCase;
import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.AssessmentCandidateResultFinalizationService;
import com.sep.vox.application.port.input.service.AssessmentHumanGradingNotificationService;
import com.sep.vox.application.port.input.service.AssessmentQuestionSecureLockService;
import com.sep.vox.application.port.input.service.AssessmentScheduleClosureService;
import com.sep.vox.application.port.input.service.ZeroScoreAssessmentResultService;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * A student fails the attempt, the grader soft-deletes that session ({@code DeleteAssessmentSessionUseCase}), and the student
 * retakes in a new session. Before this fix, {@code requirePublishReadiness} did not exclude the deleted session/result
 * from its two check loops, so the deleted session was counted as "still hanging" and blocked publishing the results of the
 * WHOLE ASSESSMENT permanently -- not only for the student who retook it.
 *
 * <p>{@code AssessmentSessionJpaEntity} does NOT use {@code @SQLRestriction} (read the note there): a
 * DELETED session is still returned normally through {@code findByCandidateId}, so this is not a test of a mere
 * assumption -- it matches the real ORM behavior exactly.
 */
class UpdateAssessmentStatusPublishReadinessTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentSessionRepository assessmentSessionRepository;
    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SubscriptionPlanRepository subscriptionPlanRepository;
    private UserContextPort userContextPort;
    private UpdateAssessmentStatusUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);
        userContextPort = mock(UserContextPort.class);
        AssessmentScheduleRepository assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);

        useCase = new UpdateAssessmentStatusUseCase(
            assessmentRepository,
            assessmentMemberRepository,
            mock(AssessmentPaperRepository.class),
            assessmentScheduleRepository,
            mock(AssessmentScheduleProctorRepository.class),
            assessmentCandidateRepository,
            assessmentSessionRepository,
            assessmentCandidateResultRepository,
            mock(AssessmentPolicyRepository.class),
            mock(AssessmentCandidateResultFinalizationService.class),
            mock(ZeroScoreAssessmentResultService.class),
            mock(SchoolUserRepository.class),
            mock(UserRoleQueryRepository.class),
            mock(AssessmentQuestionSecureLockService.class),
            schoolSubscriptionRepository,
            subscriptionPlanRepository,
            userContextPort,
            mock(EventPublisherPort.class),
            mock(ClassTestGradingAssignmentService.class),
            mock(AssessmentHumanGradingNotificationService.class),
            mock(ClassTestTokenQuotaGuardService.class),
            new AssessmentScheduleClosureService(assessmentScheduleRepository, assessmentSessionRepository));

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(candidateId);
        candidate.setAssessmentId(assessmentId);
        candidate.setStatus(AssessmentCandidateStatus.ATTENDED);
        when(assessmentCandidateRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(candidate));
    }

    /**
     * The OLD session is deleted AFTER a result exists (exactly the scenario from the bug report):
     * the result still has a row in the DB, only its status changes to DELETED, and it never moves
     * to RELEASED/INVALID by itself. The NEW session (a retake) is already graded, RELEASED.
     * Publishing must go through; the DELETED row must not be counted as "still pending".
     */
    @Test
    void should_publish_when_the_only_unresolved_result_belongs_to_a_deleted_retaken_session() {
        AssessmentSession deletedSession = session(AssessmentSessionStatus.DELETED);
        AssessmentSession retakeSession = session(AssessmentSessionStatus.GRADED);
        when(assessmentSessionRepository.findByCandidateId(candidateId))
            .thenReturn(List.of(deletedSession, retakeSession));

        AssessmentCandidateResult deletedResult = result(deletedSession.getId(), AssessmentCandidateResultStatus.DELETED);
        AssessmentCandidateResult retakeResult = result(retakeSession.getId(), AssessmentCandidateResultStatus.RELEASED);
        when(assessmentCandidateResultRepository.findBySessionId(deletedSession.getId()))
            .thenReturn(Optional.of(deletedResult));
        when(assessmentCandidateResultRepository.findBySessionId(retakeSession.getId()))
            .thenReturn(Optional.of(retakeResult));
        when(assessmentCandidateResultRepository.findByAssessmentId(assessmentId))
            .thenReturn(List.of(deletedResult, retakeResult));

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(closedAssessment()));

        assertThatCode(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "PUBLISH_RESULTS", null)))
            .doesNotThrowAnyException();
    }

    /**
     * The OLD session is deleted BEFORE it had any result (an assessment-room or technical incident,
     * deleted right away). There is no result row to be deleted -- {@code findBySessionId} really
     * returns empty, it is not DELETED. The other edge of the same bug: {@code missingResultCount}
     * must not count a deleted session as "missing a result".
     */
    @Test
    void should_publish_when_the_deleted_session_never_had_a_result_at_all() {
        AssessmentSession deletedSession = session(AssessmentSessionStatus.DELETED);
        AssessmentSession retakeSession = session(AssessmentSessionStatus.GRADED);
        when(assessmentSessionRepository.findByCandidateId(candidateId))
            .thenReturn(List.of(deletedSession, retakeSession));

        when(assessmentCandidateResultRepository.findBySessionId(deletedSession.getId()))
            .thenReturn(Optional.empty());
        AssessmentCandidateResult retakeResult = result(retakeSession.getId(), AssessmentCandidateResultStatus.RELEASED);
        when(assessmentCandidateResultRepository.findBySessionId(retakeSession.getId()))
            .thenReturn(Optional.of(retakeResult));
        when(assessmentCandidateResultRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(retakeResult));

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(closedAssessment()));

        assertThatCode(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "PUBLISH_RESULTS", null)))
            .doesNotThrowAnyException();
    }

    /**
     * Control case: a real (not deleted) PENDING_REVIEW result must still block publishing as
     * before -- the patch only lets DELETED through and does not loosen the gate for any other
     * status.
     */
    @Test
    void should_still_block_publish_when_a_real_pending_result_remains() {
        AssessmentSession pendingSession = session(AssessmentSessionStatus.GRADED);
        when(assessmentSessionRepository.findByCandidateId(candidateId)).thenReturn(List.of(pendingSession));

        AssessmentCandidateResult pendingResult = result(pendingSession.getId(), AssessmentCandidateResultStatus.PENDING_REVIEW);
        when(assessmentCandidateResultRepository.findBySessionId(pendingSession.getId()))
            .thenReturn(Optional.of(pendingResult));
        when(assessmentCandidateResultRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(pendingResult));

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(closedAssessment()));

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentStatusCommand(assessmentId, "PUBLISH_RESULTS", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("neither RELEASED nor INVALID");
    }

    private Assessment closedAssessment() {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        assessment.setKind(AssessmentKind.CENTRALIZED);
        assessment.setStatus(AssessmentStatus.CLOSED);
        assessment.setAssessmentPolicyId(null);
        return assessment;
    }

    private AssessmentSession session(AssessmentSessionStatus status) {
        AssessmentSession session = new AssessmentSession();
        session.setId(UUID.randomUUID());
        session.setAssessmentId(assessmentId);
        session.setCandidateId(candidateId);
        session.setStatus(status);
        return session;
    }

    private AssessmentCandidateResult result(UUID sessionId, AssessmentCandidateResultStatus status) {
        AssessmentCandidateResult result = new AssessmentCandidateResult();
        result.setId(UUID.randomUUID());
        result.setAssessmentId(assessmentId);
        result.setCandidateId(candidateId);
        result.setSessionId(sessionId);
        result.setStatus(status);
        return result;
    }
}
