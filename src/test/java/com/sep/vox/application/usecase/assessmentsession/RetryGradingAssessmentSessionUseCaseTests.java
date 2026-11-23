package com.sep.vox.application.usecase.assessmentsession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.RetryGradingAssessmentSessionCommand;
import com.sep.vox.application.port.input.service.AssessmentSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.assessmentsession.RetryGradingAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.SubmitAssessmentSessionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * Định mức "trường được nhờ AI chấm lại một lượt".
 *
 * <p>Điểm dễ sai nhất KHÔNG phải con số 1, mà là AI TIÊU vào định mức đó: nếu lượt của quản trị hệ
 * thống (và sau này là job tự chạy lại khi có sự cố diện rộng) cũng trừ, thì một lần AI hỏng hàng
 * loạt sẽ âm thầm đốt sạch lượt của mọi trường rồi ép hàng trăm bài sang chấm tay — vì lỗi của chính
 * nền tảng.
 */
class RetryGradingAssessmentSessionUseCaseTests {

    private static final UUID SESSION_ID = UUID.randomUUID();

    private AssessmentSessionRepository assessmentSessionRepository;
    private UserContextPort userContextPort;
    private SubmitAssessmentSessionUseCase submitAssessmentSessionUseCase;
    private RetryGradingAssessmentSessionUseCase useCase;
    private AssessmentSession session;

    @BeforeEach
    void setUp() {
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        AssessmentCandidateRepository assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        AssessmentCandidateResultRepository assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        AssessmentRepository assessmentRepository = mock(AssessmentRepository.class);
        AssessmentSessionModerationAccessService moderationAccessService = mock(AssessmentSessionModerationAccessService.class);
        userContextPort = mock(UserContextPort.class);
        submitAssessmentSessionUseCase = mock(SubmitAssessmentSessionUseCase.class);

        session = AssessmentSession.builder()
                .id(SESSION_ID)
                .assessmentId(UUID.randomUUID())
                .candidateId(UUID.randomUUID())
                .paperId(UUID.randomUUID())
                .startedAt(Instant.now().minusSeconds(3600))
                .submittedAt(Instant.now().minusSeconds(600))
                .status(AssessmentSessionStatus.GRADING_FAILED)
                .flagged(false)
                .flagReason(null)
                .build();

        Assessment assessment = new Assessment();
        assessment.setId(UUID.randomUUID());
        assessment.setStatus(AssessmentStatus.CLOSED);

        when(assessmentSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(assessmentCandidateRepository.findById(any())).thenReturn(Optional.of(new AssessmentCandidate()));
        when(assessmentRepository.findById(any())).thenReturn(Optional.of(assessment));
        when(assessmentCandidateResultRepository.findBySessionId(any())).thenReturn(Optional.empty());

        useCase = new RetryGradingAssessmentSessionUseCase(assessmentSessionRepository, assessmentCandidateRepository,
            assessmentCandidateResultRepository, assessmentRepository, moderationAccessService, userContextPort,
            submitAssessmentSessionUseCase);
    }

    @Test
    void shouldLetTheSchoolAskForAnAiRegradeOnce() {
        when(userContextPort.isSystemAdmin()).thenReturn(false);

        useCase.execute(new RetryGradingAssessmentSessionCommand(SESSION_ID));

        assertThat(session.getSchoolRegradeCount()).isEqualTo(1);
        verify(submitAssessmentSessionUseCase).execute(any());
    }

    /** Lượt thứ hai hỏng tiếp là bằng chứng nguyên nhân mang tính tất định — đường ra là chấm tay. */
    @Test
    void shouldRefuseASecondSchoolRegradeAndPointAtHumanGrading() {
        when(userContextPort.isSystemAdmin()).thenReturn(false);
        session.setSchoolRegradeCount(1);

        assertThatThrownBy(() -> useCase.execute(new RetryGradingAssessmentSessionCommand(SESSION_ID)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("chuyển sang chấm tay");

        verify(submitAssessmentSessionUseCase, never()).execute(any());
    }

    /**
     * Lượt của quản trị hệ thống là lượt khắc phục sự cố NỀN TẢNG. Nó không được tiêu định mức của
     * trường, nếu không một sự cố diện rộng sẽ ép hàng loạt bài sang chấm tay vì lỗi của mình.
     */
    @Test
    void shouldNotSpendTheSchoolAllowanceOnASystemAdminRetry() {
        when(userContextPort.isSystemAdmin()).thenReturn(true);

        useCase.execute(new RetryGradingAssessmentSessionCommand(SESSION_ID));
        useCase.execute(new RetryGradingAssessmentSessionCommand(SESSION_ID));

        assertThat(session.getSchoolRegradeCount()).isZero();
        verify(submitAssessmentSessionUseCase, org.mockito.Mockito.times(2)).execute(any());
    }

    /** Quản trị hệ thống vẫn chấm lại được sau khi trường đã dùng hết lượt của mình. */
    @Test
    void shouldStillAllowASystemAdminRetryAfterTheSchoolAllowanceIsSpent() {
        when(userContextPort.isSystemAdmin()).thenReturn(true);
        session.setSchoolRegradeCount(1);

        useCase.execute(new RetryGradingAssessmentSessionCommand(SESSION_ID));

        verify(submitAssessmentSessionUseCase).execute(any());
    }
}
