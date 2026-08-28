package com.sep.vox.application.usecase.examsession;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RecordAiUsageCommand;
import com.sep.vox.application.port.input.command.ReportAiUsageCommand;
import com.sep.vox.application.port.input.usecase.examsession.ReportAiUsageUseCase;
import com.sep.vox.application.port.input.usecase.metering.RecordAiUsageUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.metering.AiUsageType;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * ReportAiUsageUseCase là đường REST song song với AiUsageRecordedConsumer (Kafka) -- điểm khác
 * biệt duy nhất cần test riêng là ownership check (học viên chỉ báo được usage cho CHÍNH phiên thi
 * của mình); phần idempotency/lưu trữ đã có RecordAiUsageUseCaseTests bao phủ, ở đây chỉ verify nó
 * được gọi đúng số lần.
 */
class ReportAiUsageUseCaseTests {

    private UserContextPort userContextPort;
    private ExamSessionRepository examSessionRepository;
    private ExamCandidateRepository examCandidateRepository;
    private RecordAiUsageUseCase recordAiUsageUseCase;
    private ReportAiUsageUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        examSessionRepository = mock(ExamSessionRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        recordAiUsageUseCase = mock(RecordAiUsageUseCase.class);
        useCase = new ReportAiUsageUseCase(
            userContextPort, examSessionRepository, examCandidateRepository, recordAiUsageUseCase);

        var session = new ExamSession();
        session.setId(sessionId);
        session.setCandidateId(candidateId);
        session.setStatus(ExamSessionStatus.IN_PROGRESS);

        var candidate = new ExamCandidate();
        candidate.setId(candidateId);
        candidate.setStudentId(userId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(examSessionRepository.findByIdAndResumable(sessionId)).thenReturn(Optional.of(session));
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
    }

    private RecordAiUsageCommand usageItem() {
        return new RecordAiUsageCommand(
            sessionId, UUID.randomUUID(), UUID.randomUUID(), AiUsageType.DURATION,
            "azure_tts", null, null, null, null, null,
            1200L, "{\"amount\":0.006}", new BigDecimal("0.0072"), Instant.now()
        );
    }

    @Test
    void delegatesEachUsageItemToRecordAiUsageUseCase() {
        var items = List.of(usageItem(), usageItem());

        useCase.execute(new ReportAiUsageCommand(sessionId, items));

        verify(recordAiUsageUseCase, times(2)).execute(any());
    }

    @Test
    void rejectsReportForASessionTheCallerDoesNotOwn() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> useCase.execute(new ReportAiUsageCommand(sessionId, List.of(usageItem()))))
            .isInstanceOf(ForbiddenException.class);
        verify(recordAiUsageUseCase, never()).execute(any());
    }

    @Test
    void rejectsReportForASessionThatIsNotResumable() {
        when(examSessionRepository.findByIdAndResumable(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ReportAiUsageCommand(sessionId, List.of(usageItem()))))
            .isInstanceOf(NotFoundException.class);
        verify(recordAiUsageUseCase, never()).execute(any());
    }
}
