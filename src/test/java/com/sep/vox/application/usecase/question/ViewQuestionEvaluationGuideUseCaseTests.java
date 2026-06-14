package com.sep.vox.application.usecase.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.usecase.question.ViewQuestionEvaluationGuideUseCase;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;

class ViewQuestionEvaluationGuideUseCaseTests {

    private QuestionEvaluationGuideRepository guideRepository;
    private ViewQuestionEvaluationGuideUseCase useCase;

    @BeforeEach
    void setUp() {
        guideRepository = mock(QuestionEvaluationGuideRepository.class);
        useCase = new ViewQuestionEvaluationGuideUseCase(guideRepository);
    }

    @Test
    void execute_should_map_guide_to_dto() {
        var questionId = UUID.randomUUID();
        when(guideRepository.findByQuestionId(questionId)).thenReturn(Optional.of(
            new QuestionEvaluationGuide(UUID.randomUUID(), questionId, "Expected", "Key", "Accept", "Off", "Hints", "Mistakes")
        ));

        var result = useCase.execute(questionId);

        assertThat(result).isPresent();
        assertThat(result.get().questionId()).isEqualTo(questionId);
        assertThat(result.get().expectedContent()).isEqualTo("Expected");
    }
}
