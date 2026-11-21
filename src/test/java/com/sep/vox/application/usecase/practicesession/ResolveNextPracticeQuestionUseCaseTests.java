package com.sep.vox.application.usecase.practicesession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.input.service.PracticeQuestionSelectionService;
import com.sep.vox.application.port.input.service.PracticeQuestionSelectionService.NextQuestionSelection;
import com.sep.vox.application.port.input.service.ResolveNextPracticeQuestionClaimService;
import com.sep.vox.application.port.input.service.ResolveNextPracticeQuestionClaimService.Claim;
import com.sep.vox.application.port.input.service.ResolveNextPracticeQuestionPersistenceService;
import com.sep.vox.application.port.input.usecase.practicesession.ResolveNextPracticeQuestionUseCase;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.PracticeGenerationConfigPort;
import com.sep.vox.application.query.dto.PracticeFocusInfo;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.model.personalization.PracticeTopic;

@ExtendWith(MockitoExtension.class)
class ResolveNextPracticeQuestionUseCaseTests {

    private final UUID sessionId = UUID.randomUUID();

    @Mock
    private ResolveNextPracticeQuestionClaimService claimService;
    @Mock
    private PracticeQuestionSelectionService selectionService;
    @Mock
    private ResolveNextPracticeQuestionPersistenceService persistenceService;
    @Mock
    private CacheManagerPort cacheManagerPort;
    @Mock
    private PracticeGenerationConfigPort generationConfig;

    private ResolveNextPracticeQuestionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResolveNextPracticeQuestionUseCase(claimService, selectionService, persistenceService,
            cacheManagerPort, generationConfig, new FakeJsonSerializationPort());
    }

    @Test
    void execute_should_return_the_suggested_ideas_of_a_valid_json_array() {
        assertThat(suggestedIdeasFor("[\"park\",\"friends\"]")).containsExactly("park", "friends");
    }

    @Test
    void execute_should_return_no_suggested_ideas_when_the_value_is_blank() {
        assertThat(suggestedIdeasFor("  ")).isEmpty();
    }

    @Test
    void execute_should_return_no_suggested_ideas_when_the_value_is_malformed_json() {
        assertThat(suggestedIdeasFor("[\"park\"")).isEmpty();
    }

    @Test
    void execute_should_return_no_suggested_ideas_when_the_value_is_a_json_object() {
        assertThat(suggestedIdeasFor("{\"idea\":\"park\"}")).isEmpty();
    }

    private List<String> suggestedIdeasFor(String suggestedIdeasJson) {
        PracticeQuestion question = PracticeQuestion.builder()
            .id(UUID.randomUUID())
            .questionText("Describe your weekend.")
            .difficultyRank(3)
            .maxResponseSeconds(60)
            .minResponseSeconds(10)
            .suggestedIdeasJson(suggestedIdeasJson)
            .build();
        when(generationConfig.onlineBudget()).thenReturn(Duration.ofSeconds(10));
        when(cacheManagerPort.saveIfAbsentAndGet(any(), any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        when(claimService.claim(sessionId)).thenReturn(claimWithoutIdempotentQuestion());
        when(selectionService.resolveNextQuestion(any(), any(), any(), anyInt(),
            anyInt(), any(), any()))
            .thenReturn(Optional.of(new NextQuestionSelection(question, 1, "GRAMMAR", null, 3)));

        ResolveNextPracticeQuestionUseCase.Result result = useCase.execute(sessionId);

        assertThat(result.status()).isEqualTo("ok");
        return result.question().suggestedIdeas();
    }

    private Claim claimWithoutIdempotentQuestion() {
        return new Claim(UUID.randomUUID(), UUID.randomUUID(), PracticeTopic.builder().id(UUID.randomUUID()).build(),
            new PracticeFocusInfo(List.of("GRAMMAR"), Map.of()), List.of(), null, 0, null, 0, 1000, 3, 6, null);
    }
}
