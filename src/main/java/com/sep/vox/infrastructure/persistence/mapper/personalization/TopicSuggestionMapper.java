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
        var entity = new TopicSuggestionJpaEntity(
            // Sinh id ở đây khi tầng gọi để null. TopicSuggestionJpaEntity chỉ có @Id, KHÔNG
            // có @GeneratedValue, nên persist() với id null thì Hibernate ném
            // IdentifierGenerationException -- và TopicSuggestionService.refreshSuggestions
            // đúng là truyền null. Nghĩa là TopicSuggestionRefreshJob chưa từng tạo được gợi ý
            // nào: mỗi lượt chạy đều chết ở dòng save đầu tiên.
            //
            // Sinh ở tầng ánh xạ, cùng cách các adapter khác trong repo đang làm
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
