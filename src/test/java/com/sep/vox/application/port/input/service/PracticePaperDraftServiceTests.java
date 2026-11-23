package com.sep.vox.application.port.input.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.PracticeGenerationExecutorPort;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaper;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaperDraft;

@ExtendWith(MockitoExtension.class)
class PracticePaperDraftServiceTests {

    private static final String GENERIC_MESSAGE = "Không dựng được đề luyện lúc này, vui lòng thử lại.";

    @Mock
    private PracticeGenerationExecutorPort practiceGenerationExecutorPort;

    @Mock
    private CacheManagerPort cacheManagerPort;

    @InjectMocks
    private PracticePaperDraftService practicePaperDraftService;

    @Test
    void startReturnsBusinessRuleViolationErrorCodeWithTheOriginalMessage() {
        PracticePaperDraft draft = startFailingWith(new BusinessRuleViolationException("Quota is used up."));

        assertThat(draft.status()).isEqualTo("FAILED");
        assertThat(draft.reason()).isEqualTo("Quota is used up.");
        assertThat(draft.errorCode()).isEqualTo("BUSINESS_RULE_VIOLATION");
    }

    @Test
    void startKeepsTheMessageAndHasNoErrorCodeForIllegalArgumentException() {
        PracticePaperDraft draft = startFailingWith(new IllegalArgumentException("Topic has no questions."));

        assertThat(draft.reason()).isEqualTo("Topic has no questions.");
        assertThat(draft.errorCode()).isNull();
    }

    @Test
    void startReturnsTheGenericMessageForAnUnknownError() {
        PracticePaperDraft draft = startFailingWith(new UnsupportedOperationException("internal detail"));

        assertThat(draft.reason()).isEqualTo(GENERIC_MESSAGE);
        assertThat(draft.errorCode()).isNull();
    }

    @Test
    void startReturnsTheGenericMessageForAnIllegalStateException() {
        PracticePaperDraft draft = startFailingWith(new IllegalStateException("internal detail"));

        assertThat(draft.reason()).isEqualTo(GENERIC_MESSAGE);
        assertThat(draft.errorCode()).isNull();
    }

    private PracticePaperDraft startFailingWith(RuntimeException failure) {
        doReturn(CompletableFuture.<PracticePaper>failedFuture(failure))
            .when(practiceGenerationExecutorPort).supplyAsync(any());
        Supplier<PracticePaper> buildPaper = () -> null;
        return practicePaperDraftService.start(UUID.randomUUID(), buildPaper);
    }
}
