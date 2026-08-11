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
    /**
     * Chiều sở thích của ĐÚNG những chủ đề được hỏi tới, khoá theo id.
     *
     * <p>Thay cho {@code findAllTopicDimensions()} cũ (gỡ 2026-08-11) vốn gọi {@code findAll()}
     * thuần: nạp MỌI chủ đề, KỂ CẢ đã tắt, dưới dạng entity đầy đủ (gồm cột {@code description}
     * kiểu TEXT) chỉ để dựng một map hai trường -- và chạy sau MỖI buổi luyện.
     *
     * <p>Cố ý KHÔNG lọc {@code active}: bản cũ cũng không lọc, và sự kiện quan tâm nằm trên chủ đề
     * đã tắt vẫn phải được tính. Thêm lọc ở đây là đổi kết quả điểm chiều một cách âm thầm.
     *
     * @param topicIds rỗng thì trả map rỗng -- không được hiểu thành "lấy tất cả"
     */
    Map<UUID, String> findDimensionsByIds(java.util.Collection<UUID> topicIds);

    /**
     * Danh thiếp (id, tên, chiều) của chủ đề đang hoạt động -- cho phép chống trùng theo tên.
     *
     * <p>Thay {@code findAllActive()} cũ (gỡ 2026-08-11) vốn trả entity đầy đủ kèm cột
     * {@code description} kiểu TEXT, trong khi chỗ dùng chỉ đọc ba trường này.
     */
    List<com.sep.vox.application.query.dto.TopicNameCardInfo> findActiveNameCards();

    /** Cỡ kho chủ đề nuôi lô chào (đã trừ chủ đề vật chất hoá từ ngân hàng đề của trường). */
    long countOfferablePool();

    // GỠ 2026-08-11: findAllActiveOrderByName(). Chỗ dùng duy nhất là TopicSuggestionService
    // .topicNames(), vốn gửi cả danh sách tên xuống prompt LLM -- đã bỏ cùng đợt.

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
