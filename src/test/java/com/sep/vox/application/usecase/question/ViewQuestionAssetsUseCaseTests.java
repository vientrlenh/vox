package com.sep.vox.application.usecase.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.usecase.question.ViewQuestionAssetsUseCase;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;
import com.sep.vox.domain.repository.QuestionAssetRepository;

class ViewQuestionAssetsUseCaseTests {

    private QuestionAssetRepository assetRepository;
    private ViewQuestionAssetsUseCase useCase;

    @BeforeEach
    void setUp() {
        assetRepository = mock(QuestionAssetRepository.class);
        useCase = new ViewQuestionAssetsUseCase(assetRepository);
    }

    @Test
    void execute_should_map_assets_to_dto() {
        var questionId = UUID.randomUUID();
        when(assetRepository.findByQuestionId(questionId)).thenReturn(List.of(
            new QuestionAsset(UUID.randomUUID(), questionId, "Image", null, "Alt", QuestionAssetType.IMAGE, "url", null, "desc", 1)
        ));

        var result = useCase.execute(questionId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).questionId()).isEqualTo(questionId);
        assertThat(result.get(0).type()).isEqualTo("IMAGE");
    }
}
