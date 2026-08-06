package com.sep.vox.domain.repository.personalization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.PracticeQuestion;

public interface PracticeQuestionRepository {

    Optional<PracticeQuestion> findById(UUID id);

    
    List<PracticeQuestion> findUnseenByTopicAndCriterionAndRankRange(
        UUID topicId,
        UUID studentId,
        String criterion,
        String tense,
        int rankMin,
        int rankMax
    );

    List<PracticeQuestion> findByIds(List<UUID> ids);

    PracticeQuestion save(PracticeQuestion question);

    void saveGenerated(PracticeQuestion question);

    /**
     * Câu của chủ đề này đã chết VĨNH VIỄN với học sinh -- gửi xuống Python để loại khỏi phép
     * so trùng lúc sinh câu mới, tránh khoá cứng. Xem chú thích dài ở
     * {@code SpringDataPracticeQuestionRepository.findPermanentlyExhaustedIds}.
     */
    List<UUID> findPermanentlyExhaustedIds(UUID topicId, UUID studentId);

    void incrementUsageCount(UUID id);

    /** Trả lại lượt dùng khi câu được chọn nhưng học sinh chưa bao giờ trả lời. */
    void decrementUsageCount(UUID id);

    record QuestionEvaluationInfo(
        String questionText,
        String evaluationGuideJson,
        String questionType,
        Integer minResponseSeconds,
        Integer maxResponseSeconds,
        String topicName,
        String topicDescription
    ) {
    }

    Optional<QuestionEvaluationInfo> findQuestionWithTopic(UUID questionId);
}
