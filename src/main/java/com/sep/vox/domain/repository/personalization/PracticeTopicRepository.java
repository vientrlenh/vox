package com.sep.vox.domain.repository.personalization;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.query.dto.QuestionTopicInfo;
import com.sep.vox.domain.model.personalization.PracticeTopic;

public interface PracticeTopicRepository {

    Optional<PracticeTopic> findTopicById(UUID id);

    boolean existsActiveById(UUID id);

    PracticeTopic save(PracticeTopic topic);

    /** topicId -> interestDimension, cho toàn bộ topic active. Dùng để quy đổi sự kiện chủ đề sang điểm theo dimension. */
    Map<UUID, String> findAllTopicDimensions();

    List<PracticeTopic> findAllActive();

    /** Cỡ kho chủ đề nuôi lô chào (đã trừ chủ đề vật chất hoá từ ngân hàng đề của trường). */
    long countOfferablePool();

    List<PracticeTopic> findAllActiveOrderByName();

    Optional<PracticeTopic> findByNormalizedName(String normalizedName);

    List<String> findExhaustedTopicNames(UUID studentId);

    /** dimension -> điểm sở thích hiện tại của học sinh, theo hồ sơ mới nhất. */
    Map<String, Double> findInterestScoresByDimension(UUID studentId);

    Optional<PracticeTopic> findBySourceQuestionTopicId(UUID sourceQuestionTopicId);

    /** Topic đã PUBLISHED trong ngân hàng câu hỏi (question_bank/question_topic) của đúng
     * trường + khối hiện tại của học sinh -- nguồn topic cho luyện tập EXAM_PREP. Bank chưa
     * gắn khối nào áp dụng cho mọi khối trong trường đó. */
    List<QuestionTopicInfo> findPublishedExamTopics(UUID schoolId, UUID gradeId);
}
