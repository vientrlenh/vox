package com.sep.vox.domain.repository.personalization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.PracticeQuestion;

public interface PracticeQuestionRepository {

    Optional<PracticeQuestion> findById(UUID id);

    List<PracticeQuestion> findUnseenByTopic(
        UUID topicId,
        UUID studentId
    );

    /**
     * Loại câu theo mức đạt (band), không phải theo đã-từng-thấy (gói 11 mục 3): câu đã đạt
     * band mục tiêu (matched_band_code >= 'BAC_' || difficulty_rank, đúng target_criterion_code)
     * bị loại vĩnh viễn; câu chưa đạt chỉ bị loại trong 24h kể từ seen_at gần nhất.
     */
    List<PracticeQuestion> findUnseenByTopicAndCriterionAndRankRange(
        UUID topicId,
        UUID studentId,
        String criterion,
        int rankMin,
        int rankMax
    );

    List<PracticeQuestion> findUnseenByIds(
        List<UUID> ids,
        UUID studentId
    );

    List<PracticeQuestion> findByIds(List<UUID> ids);

    PracticeQuestion save(PracticeQuestion question);

    void saveGenerated(PracticeQuestion question);

    void incrementUsageCount(UUID id);

    record QuestionEvaluationInfo(
        String questionText,
        String evaluationGuideJson,
        Integer maxResponseSeconds,
        String topicName,
        String topicDescription
    ) {
    }

    Optional<QuestionEvaluationInfo> findQuestionWithTopic(UUID questionId);
}
