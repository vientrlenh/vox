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
     * band mục tiêu (result_band_order của matched_band_code >= difficulty_rank, đúng
     * target_criterion_code) bị loại vĩnh viễn; câu chưa đạt chỉ bị loại trong 24h kể từ
     * seen_at gần nhất.
     *
     * So sánh theo SỐ thứ tự bậc, không nối chuỗi mã bậc -- mã bậc do framework của trường
     * quyết định (VSTEP BAC_*, CEFR A1..C2, IELTS...), so chuỗi là khoá cứng vào VSTEP.
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
        String questionType,
        Integer minResponseSeconds,
        Integer maxResponseSeconds,
        String topicName,
        String topicDescription
    ) {
    }

    Optional<QuestionEvaluationInfo> findQuestionWithTopic(UUID questionId);
}
