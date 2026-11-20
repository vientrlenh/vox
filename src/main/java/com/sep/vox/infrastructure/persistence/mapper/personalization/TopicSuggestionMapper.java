package com.sep.vox.infrastructure.persistence.mapper.personalization;

import java.util.UUID;

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
        TopicSuggestionJpaEntity entity = new TopicSuggestionJpaEntity(
            // Generate the id here when the calling layer leaves it null. TopicSuggestionJpaEntity only has @Id and NO
            // @GeneratedValue, so persist() with a null id makes Hibernate throw
            // IdentifierGenerationException -- and TopicSuggestionService.refreshSuggestions
            // does pass null. That means TopicSuggestionRefreshJob has never been able to create a suggestion:
            // every run died on the first save line.
            //
            // Generated in the mapping layer, the same way the other adapters in the repo do it
            // (PracticeItemResponseRepositoryImpl, TopicInterestScoreRepositoryImpl).
            suggestion.getId() == null ? UUID.randomUUID() : suggestion.getId(),
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
