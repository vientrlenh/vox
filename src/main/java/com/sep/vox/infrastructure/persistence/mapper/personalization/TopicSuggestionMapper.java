package com.sep.vox.infrastructure.persistence.mapper.personalization;

import com.sep.vox.domain.model.personalization.TopicSuggestion;
import com.sep.vox.infrastructure.persistence.entity.TopicSuggestionJpaEntity;

public final class TopicSuggestionMapper {

    private TopicSuggestionMapper() {
    }

    public static TopicSuggestion toDomain(
            TopicSuggestionJpaEntity entity) {
        return new TopicSuggestion(
            entity.getId(),
            entity.getStudentId(),
            entity.getSuggestedTopicName(),
            entity.getKeyword(),
            entity.getInterestDimension(),
            entity.getCurriculumGroup(),
            entity.getConfidence(),
            entity.getReasonText(),
            entity.getEvidenceJson(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getRespondedAt()
        );
    }

    public static TopicSuggestionJpaEntity toJpa(
            TopicSuggestion suggestion) {
        var entity = new TopicSuggestionJpaEntity(
            suggestion.getId(),
            suggestion.getStudentId(),
            suggestion.getSuggestedTopicName(),
            suggestion.getKeyword(),
            suggestion.getInterestDimension(),
            suggestion.getCurriculumGroup(),
            suggestion.getConfidence(),
            suggestion.getReasonText(),
            suggestion.getEvidenceJson(),
            suggestion.getStatus(),
            suggestion.getCreatedAt()
        );
        entity.setRespondedAt(suggestion.getRespondedAt());
        return entity;
    }
}
