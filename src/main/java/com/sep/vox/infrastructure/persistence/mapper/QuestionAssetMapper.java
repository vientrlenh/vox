package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;
import com.sep.vox.infrastructure.persistence.entity.question.QuestionAssetJpaEntity;

public final class QuestionAssetMapper {

    public static QuestionAsset toDomain(QuestionAssetJpaEntity jpa) {
        return QuestionAsset.builder()
                .id(jpa.getId())
                .questionId(jpa.getQuestionId())
                .title(jpa.getTitle())
                .durationSeconds(jpa.getDurationSeconds())
                .altText(jpa.getAltText())
                .type(QuestionAssetType.valueOf(jpa.getType()))
                .url(jpa.getUrl())
                .transcript(jpa.getTranscript())
                .description(jpa.getDescription())
                .order(jpa.getOrder())
                .build();
    }

    public static QuestionAssetJpaEntity toJpa(QuestionAsset domain) {
        return QuestionAssetJpaEntity.builder()
                .id(domain.getId())
                .questionId(domain.getQuestionId())
                .title(domain.getTitle())
                .durationSeconds(domain.getDurationSeconds())
                .altText(domain.getAltText())
                .type(domain.getType().name())
                .url(domain.getUrl())
                .transcript(domain.getTranscript())
                .description(domain.getDescription())
                .order(domain.getOrder())
                .build();
    }
}
