package com.sep.vox.application.usecase.exam;

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

import com.sep.vox.application.port.input.command.UpdateExamStatusCommand;
import com.sep.vox.application.port.input.usecase.exam.ExamQuestionSecureLockService;
import com.sep.vox.application.port.input.usecase.exam.UpdateExamStatusUseCase;
import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.ExamCandidateResultFinalizationService;
import com.sep.vox.application.port.input.service.ExamHumanGradingNotificationService;
import com.sep.vox.application.port.input.service.ExamScheduleClosureService;
import com.sep.vox.application.port.input.service.ZeroScoreExamResultService;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Học sinh thi hỏng, giám khảo xoá mềm phiên đó ({@code DeleteExamSessionUseCase}), học sinh thi
 * lại ở một phiên mới. Trước bản vá này, {@code requirePublishReadiness} không loại phiên/kết quả
 * đã xoá ra khỏi hai vòng kiểm, nên phiên đã xoá bị tính là "còn treo" và chặn công bố kết quả của
 * CẢ KỲ THI vĩnh viễn -- không riêng gì thí sinh đã thi lại.
 *
 * <p>{@code ExamSessionJpaEntity} KHÔNG dùng {@code @SQLRestriction} (đọc chú thích ở đó): phiên
 * DELETED vẫn trả về bình thường qua {@code findByCandidateId}, nên đây không phải test giả định
 * suông -- nó khớp đúng hành vi ORM thật.
 */
class UpdateExamStatusPublishReadinessTests {

    private ExamRepository examRepository;
    private ExamMemberRepository examMemberRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamSessionRepository examSessionRepository;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SubscriptionPlanRepository subscriptionPlanRepository;
    private UserContextPort userContextPort;
    private UpdateExamStatusUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examSessionRepository = mock(ExamSessionRepository.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);
        userContextPort = mock(UserContextPort.class);
        ExamScheduleRepository examScheduleRepository = mock(ExamScheduleRepository.class);

        useCase = new UpdateExamStatusUseCase(
            examRepository,
            examMemberRepository,
            mock(ExamPaperRepository.class),
            examScheduleRepository,
            mock(ExamScheduleProctorRepository.class),
            examCandidateRepository,
            examSessionRepository,
            examCandidateResultRepository,
            mock(AssessmentPolicyRepository.class),
            mock(ExamCandidateResultFinalizationService.class),
            mock(ZeroScoreExamResultService.class),
            mock(SchoolUserRepository.class),
            mock(UserRoleQueryRepository.class),
            mock(ExamQuestionSecureLockService.class),
            schoolSubscriptionRepository,
            subscriptionPlanRepository,
            userContextPort,
            mock(EventPublisherPort.class),
            mock(ClassTestGradingAssignmentService.class),
            mock(ExamHumanGradingNotificationService.class),
            mock(ClassTestTokenQuotaGuardService.class),
            new ExamScheduleClosureService(examScheduleRepository, examSessionRepository));

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExamCandidate candidate = new ExamCandidate();
        candidate.setId(candidateId);
        candidate.setExamId(examId);
        candidate.setStatus(ExamCandidateStatus.ATTENDED);
        when(examCandidateRepository.findByExamId(examId)).thenReturn(List.of(candidate));
    }

    /**
     * The OLD session is deleted AFTER a result exists (exactly the scenario from the bug report):
     * the result still has a row in the DB, only its status changes to DELETED, and it never moves
     * to RELEASED/INVALID by itself. The NEW session (a retake) is already graded, RELEASED.
     * Publishing must go through; the DELETED row must not be counted as "still pending".
     */
    @Test
    void should_publish_when_the_only_unresolved_result_belongs_to_a_deleted_retaken_session() {
        ExamSession deletedSession = session(ExamSessionStatus.DELETED);
        ExamSession retakeSession = session(ExamSessionStatus.GRADED);
        when(examSessionRepository.findByCandidateId(candidateId))
            .thenReturn(List.of(deletedSession, retakeSession));

        ExamCandidateResult deletedResult = result(deletedSession.getId(), ExamCandidateResultStatus.DELETED);
        ExamCandidateResult retakeResult = result(retakeSession.getId(), ExamCandidateResultStatus.RELEASED);
        when(examCandidateResultRepository.findBySessionId(deletedSession.getId()))
            .thenReturn(Optional.of(deletedResult));
        when(examCandidateResultRepository.findBySessionId(retakeSession.getId()))
            .thenReturn(Optional.of(retakeResult));
        when(examCandidateResultRepository.findByExamId(examId))
            .thenReturn(List.of(deletedResult, retakeResult));

        when(examRepository.findById(examId)).thenReturn(Optional.of(closedExam()));

        assertThatCode(() -> useCase.execute(new UpdateExamStatusCommand(examId, "PUBLISH_RESULTS", null)))
            .doesNotThrowAnyException();
    }

    /**
     * The OLD session is deleted BEFORE it had any result (an exam-room or technical incident,
     * deleted right away). There is no result row to be deleted -- {@code findBySessionId} really
     * returns empty, it is not DELETED. The other edge of the same bug: {@code missingResultCount}
     * must not count a deleted session as "missing a result".
     */
    @Test
    void should_publish_when_the_deleted_session_never_had_a_result_at_all() {
        ExamSession deletedSession = session(ExamSessionStatus.DELETED);
        ExamSession retakeSession = session(ExamSessionStatus.GRADED);
        when(examSessionRepository.findByCandidateId(candidateId))
            .thenReturn(List.of(deletedSession, retakeSession));

        when(examCandidateResultRepository.findBySessionId(deletedSession.getId()))
            .thenReturn(Optional.empty());
        ExamCandidateResult retakeResult = result(retakeSession.getId(), ExamCandidateResultStatus.RELEASED);
        when(examCandidateResultRepository.findBySessionId(retakeSession.getId()))
            .thenReturn(Optional.of(retakeResult));
        when(examCandidateResultRepository.findByExamId(examId)).thenReturn(List.of(retakeResult));

        when(examRepository.findById(examId)).thenReturn(Optional.of(closedExam()));

        assertThatCode(() -> useCase.execute(new UpdateExamStatusCommand(examId, "PUBLISH_RESULTS", null)))
            .doesNotThrowAnyException();
    }

    /**
     * Control case: a real (not deleted) PENDING_REVIEW result must still block publishing as
     * before -- the patch only lets DELETED through and does not loosen the gate for any other
     * status.
     */
    @Test
    void should_still_block_publish_when_a_real_pending_result_remains() {
        ExamSession pendingSession = session(ExamSessionStatus.GRADED);
        when(examSessionRepository.findByCandidateId(candidateId)).thenReturn(List.of(pendingSession));

        ExamCandidateResult pendingResult = result(pendingSession.getId(), ExamCandidateResultStatus.PENDING_REVIEW);
        when(examCandidateResultRepository.findBySessionId(pendingSession.getId()))
            .thenReturn(Optional.of(pendingResult));
        when(examCandidateResultRepository.findByExamId(examId)).thenReturn(List.of(pendingResult));

        when(examRepository.findById(examId)).thenReturn(Optional.of(closedExam()));

        assertThatThrownBy(() -> useCase.execute(new UpdateExamStatusCommand(examId, "PUBLISH_RESULTS", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("RELEASED hoặc INVALID");
    }

    private Exam closedExam() {
        Exam exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setKind(ExamKind.CENTRALIZED);
        exam.setStatus(ExamStatus.CLOSED);
        exam.setAssessmentPolicyId(null);
        return exam;
    }

    private ExamSession session(ExamSessionStatus status) {
        ExamSession session = new ExamSession();
        session.setId(UUID.randomUUID());
        session.setExamId(examId);
        session.setCandidateId(candidateId);
        session.setStatus(status);
        return session;
    }

    private ExamCandidateResult result(UUID sessionId, ExamCandidateResultStatus status) {
        ExamCandidateResult result = new ExamCandidateResult();
        result.setId(UUID.randomUUID());
        result.setExamId(examId);
        result.setCandidateId(candidateId);
        result.setSessionId(sessionId);
        result.setStatus(status);
        return result;
    }
}
