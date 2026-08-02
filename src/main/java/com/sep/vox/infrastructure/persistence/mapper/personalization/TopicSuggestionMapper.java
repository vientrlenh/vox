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
            suggestion.id(),
            suggestion.studentId(),
            suggestion.suggestedTopicName(),
            suggestion.keyword(),
            suggestion.interestDimension(),
            suggestion.curriculumGroup(),
            suggestion.confidence(),
            suggestion.reasonText(),
            suggestion.evidenceJson(),
            suggestion.status(),
            suggestion.createdAt()
        );
        entity.setRespondedAt(suggestion.respondedAt());
        return entity;
    }
}
