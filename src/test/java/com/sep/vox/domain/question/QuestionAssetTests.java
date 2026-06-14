package com.sep.vox.domain.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;

class QuestionAssetTests {

    @Test
    void constructor_should_store_asset_fields() {
        var questionId = UUID.randomUUID();
        var asset = new QuestionAsset(
            questionId,
            "Visual prompt",
            20,
            "Alt text",
            QuestionAssetType.IMAGE,
            "https://vox.local/image.jpg",
            null,
            "Description",
            2
        );

        assertThat(asset.getQuestionId()).isEqualTo(questionId);
        assertThat(asset.getTitle()).isEqualTo("Visual prompt");
        assertThat(asset.getDurationSeconds()).isEqualTo(20);
        assertThat(asset.getAltText()).isEqualTo("Alt text");
        assertThat(asset.getType()).isEqualTo(QuestionAssetType.IMAGE);
        assertThat(asset.getUrl()).isEqualTo("https://vox.local/image.jpg");
        assertThat(asset.getDescription()).isEqualTo("Description");
        assertThat(asset.getOrder()).isEqualTo(2);
    }
}
