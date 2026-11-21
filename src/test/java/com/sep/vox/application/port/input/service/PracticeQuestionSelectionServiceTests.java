package com.sep.vox.application.port.input.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.input.service.PracticeQuestionSelectionService.NextQuestionSelection;
import com.sep.vox.application.port.output.PracticeGenerationConfigPort;
import com.sep.vox.application.port.output.QuestionDiversityPort;
import com.sep.vox.application.query.dto.PracticeFocusInfo;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.repository.CriterionScoreAverageRepository;
import com.sep.vox.domain.repository.PracticeQuestionRepository;

@ExtendWith(MockitoExtension.class)
class PracticeQuestionSelectionServiceTests {

    private static final String RECOUNT = "{\"reasoning_type\":\"recount\"}";
    private static final String OPINION = "{\"reasoning_type\":\"opinion\"}";

    @Mock
    private PracticeQuestionRepository questionRepository;
    @Mock
    private PracticeQuestionGenerationService generationService;
    @Mock
    private QuestionDiversityPort diversityPort;
    @Mock
    private PracticeGenerationConfigPort generationConfig;
    @Mock
    private CriterionScoreAverageRepository criterionScoreRepository;
    @Mock
    private PracticeTopicOfferEnrichmentService enrichmentService;

    private PracticeQuestionSelectionService service;
    private final PracticeTopic topic = PracticeTopic.builder().id(UUID.randomUUID()).build();

    @BeforeEach
    void setUp() {
        service = new PracticeQuestionSelectionService(questionRepository, generationService, diversityPort,
            generationConfig, criterionScoreRepository, enrichmentService, new FakeJsonSerializationPort());
    }

    @Test
    void resolveNextQuestion_should_skip_a_candidate_with_the_same_reasoning_type_as_the_previous_question() {
        PracticeQuestion sameType = question(RECOUNT);
        PracticeQuestion otherType = question(OPINION);

        assertThat(chooseAfterPreviousQuestion(RECOUNT, sameType, otherType)).isEqualTo(otherType);
    }

    @Test
    void resolveNextQuestion_should_keep_a_candidate_without_a_reasoning_type() {
        PracticeQuestion sameType = question(RECOUNT);
        PracticeQuestion withoutType = question("{\"other\":1}");

        assertThat(chooseAfterPreviousQuestion(RECOUNT, sameType, withoutType)).isEqualTo(withoutType);
    }

    @Test
    void resolveNextQuestion_should_treat_a_blank_difficulty_features_json_as_no_reasoning_type() {
        PracticeQuestion sameType = question(RECOUNT);
        PracticeQuestion blank = question("  ");

        assertThat(chooseAfterPreviousQuestion(RECOUNT, sameType, blank)).isEqualTo(blank);
    }

    @Test
    void resolveNextQuestion_should_not_skip_anything_when_the_previous_question_has_a_blank_reasoning_type() {
        PracticeQuestion candidate = question(RECOUNT);

        assertThat(chooseAfterPreviousQuestion("  ", candidate)).isEqualTo(candidate);
    }

    private PracticeQuestion chooseAfterPreviousQuestion(String previousFeaturesJson, PracticeQuestion... candidates) {
        when(generationConfig.paperTargetQuestionCount()).thenReturn(1);
        when(questionRepository.findUnseenByTopicAndCriterionAndRankRange(
            any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(List.of(candidates));
        when(diversityPort.maxSimilarities(anyList(), anyList())).thenAnswer(invocation -> {
            Map<UUID, Double> similarities = new HashMap<UUID, Double>();
            List<UUID> candidateIds = invocation.getArgument(0);
            candidateIds.forEach(id -> similarities.put(id, 0.0));
            return similarities;
        });
        Optional<NextQuestionSelection> selection = service.resolveNextQuestion(
            topic, UUID.randomUUID(), new PracticeFocusInfo(List.of("GRAMMAR"), Map.of()), 3, 6, null,
            List.of(question(previousFeaturesJson)));
        return selection.orElseThrow().question();
    }

    private PracticeQuestion question(String difficultyFeaturesJson) {
        return PracticeQuestion.builder()
            .id(UUID.randomUUID())
            .difficultyRank(3)
            .difficultyFeaturesJson(difficultyFeaturesJson)
            .build();
    }
}
