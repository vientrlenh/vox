package com.sep.vox.domain.model.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestionAssetTests {

    @Test
    void attachedToSetsTheGivenValuesWithoutId() {
        UUID questionId = UUID.randomUUID();

        QuestionAsset asset = QuestionAsset.attachedTo(questionId, "Title", 12, "Alt", QuestionAssetType.IMAGE,
                "https://cdn/x.png", "Transcript", "Description", 2);

        assertThat(asset.getQuestionId()).isEqualTo(questionId);
        assertThat(asset.getTitle()).isEqualTo("Title");
        assertThat(asset.getDurationSeconds()).isEqualTo(12);
        assertThat(asset.getAltText()).isEqualTo("Alt");
        assertThat(asset.getType()).isEqualTo(QuestionAssetType.IMAGE);
        assertThat(asset.getUrl()).isEqualTo("https://cdn/x.png");
        assertThat(asset.getTranscript()).isEqualTo("Transcript");
        assertThat(asset.getDescription()).isEqualTo("Description");
        assertThat(asset.getOrder()).isEqualTo(2);
        assertThat(asset.getId()).isNull();
    }

    @Test
    void copyToKeepsTheContentForAnotherQuestionWithoutId() {
        QuestionAsset source = QuestionAsset.attachedTo(UUID.randomUUID(), "Title", 12, "Alt",
                QuestionAssetType.IMAGE, "https://cdn/x.png", "Transcript", "Description", 2);
        source.setId(UUID.randomUUID());
        UUID targetQuestionId = UUID.randomUUID();

        QuestionAsset copy = source.copyTo(targetQuestionId);

        assertThat(copy).usingRecursiveComparison().ignoringFields("id", "questionId").isEqualTo(source);
        assertThat(copy.getQuestionId()).isEqualTo(targetQuestionId);
        assertThat(copy.getId()).isNull();
    }
}
