package com.sep.vox.application.port.input.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.output.InterestQuizGenerationPort;
import com.sep.vox.application.port.output.PracticeGenerationExecutorPort;
import com.sep.vox.domain.repository.InterestQuizItemRepository;

@ExtendWith(MockitoExtension.class)
class InterestQuizBankTopUpServiceTests {

    @Mock
    private InterestQuizItemRepository quizItemRepository;
    @Mock
    private InterestQuizGenerationPort quizGenerationPort;
    @Mock
    private InterestQuizScorer interestQuizScorer;
    @Mock
    private PracticeGenerationExecutorPort practiceGenerationExecutorPort;

    @Test
    void topUpAsync_schedulesTheTopUpThroughTheExecutorPort() {
        InterestQuizBankTopUpService service = createService();
        when(interestQuizScorer.quizDimensionCodes()).thenReturn(List.of());
        runScheduledWorkInline();

        service.topUpAsync();

        verify(practiceGenerationExecutorPort).runAsync(any(Runnable.class));
        verify(interestQuizScorer).quizDimensionCodes();
    }

    @Test
    void topUpAsync_swallowsARuntimeExceptionFromTopUp() {
        InterestQuizBankTopUpService service = createService();
        when(interestQuizScorer.quizDimensionCodes()).thenThrow(new IllegalStateException("boom"));
        runScheduledWorkInline();

        assertThatCode(service::topUpAsync).doesNotThrowAnyException();
    }

    private InterestQuizBankTopUpService createService() {
        return new InterestQuizBankTopUpService(
            quizItemRepository, quizGenerationPort, interestQuizScorer, practiceGenerationExecutorPort);
    }

    private void runScheduledWorkInline() {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(practiceGenerationExecutorPort).runAsync(any(Runnable.class));
    }
}
