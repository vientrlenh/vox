package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;
import com.sep.vox.infrastructure.persistence.entity.QuestionAssetJpaEntity;

public final class QuestionAssetMapper {

    public static QuestionAsset toDomain(QuestionAssetJpaEntity jpa) {
        return new QuestionAsset(
            jpa.getId(),
            jpa.getQuestionId(),
            jpa.getTitle(),
            jpa.getDurationSeconds(),
            jpa.getAltText(),
            QuestionAssetType.valueOf(jpa.getType()),
            jpa.getUrl(),
            jpa.getTranscript(),
            jpa.getDescription(),
            jpa.getOrder()
        );
    }

    public static QuestionAssetJpaEntity toJpa(QuestionAsset domain) {
        return new QuestionAssetJpaEntity(
            domain.getId(),
            domain.getQuestionId(),
            domain.getTitle(),
            domain.getDurationSeconds(),
            domain.getAltText(),
            domain.getType().name(),
            domain.getUrl(),
            domain.getTranscript(),
            domain.getDescription(),
            domain.getOrder()
        );
    }
}
