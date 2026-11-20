package com.sep.vox.application.usecase.examsession;

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

import com.sep.vox.application.port.input.command.RetryGradingExamSessionCommand;
import com.sep.vox.application.port.input.service.ExamSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.examsession.RetryGradingExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.SubmitExamSessionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * Định mức "trường được nhờ AI chấm lại một lượt".
 *
 * <p>Điểm dễ sai nhất KHÔNG phải con số 1, mà là AI TIÊU vào định mức đó: nếu lượt của quản trị hệ
 * thống (và sau này là job tự chạy lại khi có sự cố diện rộng) cũng trừ, thì một lần AI hỏng hàng
 * loạt sẽ âm thầm đốt sạch lượt của mọi trường rồi ép hàng trăm bài sang chấm tay — vì lỗi của chính
 * nền tảng.
 */
class RetryGradingExamSessionUseCaseTests {

    private static final UUID SESSION_ID = UUID.randomUUID();

    private ExamSessionRepository examSessionRepository;
    private UserContextPort userContextPort;
    private SubmitExamSessionUseCase submitExamSessionUseCase;
    private RetryGradingExamSessionUseCase useCase;
    private ExamSession session;

    @BeforeEach
    void setUp() {
        examSessionRepository = mock(ExamSessionRepository.class);
        var examCandidateRepository = mock(ExamCandidateRepository.class);
        var examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        var examRepository = mock(ExamRepository.class);
        var moderationAccessService = mock(ExamSessionModerationAccessService.class);
        userContextPort = mock(UserContextPort.class);
        submitExamSessionUseCase = mock(SubmitExamSessionUseCase.class);

        session = new ExamSession(SESSION_ID, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            Instant.now().minusSeconds(3600), Instant.now().minusSeconds(600),
            ExamSessionStatus.GRADING_FAILED, false, null);

        var exam = new Exam();
        exam.setId(UUID.randomUUID());
        exam.setStatus(ExamStatus.CLOSED);

        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(examCandidateRepository.findById(any())).thenReturn(Optional.of(new ExamCandidate()));
        when(examRepository.findById(any())).thenReturn(Optional.of(exam));
        when(examCandidateResultRepository.findBySessionId(any())).thenReturn(Optional.empty());

        useCase = new RetryGradingExamSessionUseCase(examSessionRepository, examCandidateRepository,
            examCandidateResultRepository, examRepository, moderationAccessService, userContextPort,
            submitExamSessionUseCase);
    }

    @Test
    void shouldLetTheSchoolAskForAnAiRegradeOnce() {
        when(userContextPort.isSystemAdmin()).thenReturn(false);

        useCase.execute(new RetryGradingExamSessionCommand(SESSION_ID));

        assertThat(session.getSchoolRegradeCount()).isEqualTo(1);
        verify(submitExamSessionUseCase).execute(any());
    }

    /** Lượt thứ hai hỏng tiếp là bằng chứng nguyên nhân mang tính tất định — đường ra là chấm tay. */
    @Test
    void shouldRefuseASecondSchoolRegradeAndPointAtHumanGrading() {
        when(userContextPort.isSystemAdmin()).thenReturn(false);
        session.setSchoolRegradeCount(1);

        assertThatThrownBy(() -> useCase.execute(new RetryGradingExamSessionCommand(SESSION_ID)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("chuyển sang chấm tay");

        verify(submitExamSessionUseCase, never()).execute(any());
    }

    /**
     * Lượt của quản trị hệ thống là lượt khắc phục sự cố NỀN TẢNG. Nó không được tiêu định mức của
     * trường, nếu không một sự cố diện rộng sẽ ép hàng loạt bài sang chấm tay vì lỗi của mình.
     */
    @Test
    void shouldNotSpendTheSchoolAllowanceOnASystemAdminRetry() {
        when(userContextPort.isSystemAdmin()).thenReturn(true);

        useCase.execute(new RetryGradingExamSessionCommand(SESSION_ID));
        useCase.execute(new RetryGradingExamSessionCommand(SESSION_ID));

        assertThat(session.getSchoolRegradeCount()).isZero();
        verify(submitExamSessionUseCase, org.mockito.Mockito.times(2)).execute(any());
    }

    /** Quản trị hệ thống vẫn chấm lại được sau khi trường đã dùng hết lượt của mình. */
    @Test
    void shouldStillAllowASystemAdminRetryAfterTheSchoolAllowanceIsSpent() {
        when(userContextPort.isSystemAdmin()).thenReturn(true);
        session.setSchoolRegradeCount(1);

        useCase.execute(new RetryGradingExamSessionCommand(SESSION_ID));

        verify(submitExamSessionUseCase).execute(any());
    }
}
