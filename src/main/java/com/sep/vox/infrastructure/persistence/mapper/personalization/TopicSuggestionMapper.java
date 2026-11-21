package com.sep.vox.infrastructure.persistence.mapper.personalization;

import java.util.UUID;

import com.sep.vox.domain.model.personalization.TopicSuggestion;
import com.sep.vox.infrastructure.persistence.entity.personalization.TopicSuggestionJpaEntity;

public final class TopicSuggestionMapper {

    private TopicSuggestionMapper() {
    }

    public static TopicSuggestion toDomain(
            TopicSuggestionJpaEntity entity) {
        return TopicSuggestion.builder()
                .id(entity.getId())
                .studentId(entity.getStudentId())
                .suggestedTopicName(entity.getSuggestedTopicName())
                .keyword(entity.getKeyword())
                .interestDimension(entity.getInterestDimension())
                .curriculumGroup(entity.getCurriculumGroup())
                .confidence(entity.getConfidence())
                .reasonText(entity.getReasonText())
                .evidenceJson(entity.getEvidenceJson())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .respondedAt(entity.getRespondedAt())
                .build();
    }

    public static TopicSuggestionJpaEntity toJpa(
            TopicSuggestion suggestion) {
        return TopicSuggestionJpaEntity.builder()
            // Generate the id here when the calling layer leaves it null. TopicSuggestionJpaEntity only has @Id and NO
            // @GeneratedValue, so persist() with a null id makes Hibernate throw
            // IdentifierGenerationException -- and TopicSuggestionService.refreshSuggestions
            // does pass null. That means TopicSuggestionRefreshJob has never been able to create a suggestion:
            // every run died on the first save line.
            //
            // Generated in the mapping layer, the same way the other adapters in the repo do it
            // (PracticeItemResponseRepositoryImpl, TopicInterestScoreRepositoryImpl).
            .id(suggestion.getId() == null ? UUID.randomUUID() : suggestion.getId())
            .studentId(suggestion.getStudentId())
            .suggestedTopicName(suggestion.getSuggestedTopicName())
            .keyword(suggestion.getKeyword())
            .interestDimension(suggestion.getInterestDimension())
            .curriculumGroup(suggestion.getCurriculumGroup())
            .confidence(suggestion.getConfidence())
            .reasonText(suggestion.getReasonText())
            .evidenceJson(suggestion.getEvidenceJson())
            .status(suggestion.getStatus())
            .createdAt(suggestion.getCreatedAt())
            .respondedAt(suggestion.getRespondedAt())
            .build();
    }
}
