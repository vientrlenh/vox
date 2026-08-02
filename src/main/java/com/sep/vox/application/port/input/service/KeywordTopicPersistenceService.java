package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.model.personalization.TopicSuggestion;
import com.sep.vox.domain.repository.personalization.PracticeTopicRepository;
import com.sep.vox.domain.repository.personalization.TopicSuggestionRepository;

/**
 * Ghi topic mới sinh từ từ khoá + ghi nhận lượt dùng hạn mức của học sinh -- HAI
 * lệnh ghi GẮN VỚI NHAU về nghiệp vụ, nên phải cùng thành công hoặc cùng huỷ:
 * nếu tạo được topic mà không ghi nhận được lượt, học sinh được thêm 1 lượt tạo
 * từ khoá miễn phí (hạn mức 3 lượt/tuần trong generateFromKeyword).
 *
 * Là bean riêng, KHÔNG phải method trong TopicSuggestionService, vì:
 * (a) self-invocation bỏ qua proxy AOP nên @Transactional sẽ không có tác dụng;
 * (b) generateFromKeyword có gọi LLM chậm ở giữa, không được nằm trong transaction.
 */
@Service
public class KeywordTopicPersistenceService {

    private final PracticeTopicRepository practiceTopicRepository;
    private final TopicSuggestionRepository topicSuggestionRepository;

    public KeywordTopicPersistenceService(
            PracticeTopicRepository practiceTopicRepository,
            TopicSuggestionRepository topicSuggestionRepository) {
        this.practiceTopicRepository = practiceTopicRepository;
        this.topicSuggestionRepository = topicSuggestionRepository;
    }

    @Transactional
    public UUID createTopicAndRecordRequest(
            UUID studentId,
            String keyword,
            String topicName,
            String dimension,
            String curriculumGroup) {
        var now = OffsetDateTime.now();
        var saved = practiceTopicRepository.save(new PracticeTopic(
            null,
            topicName,
            TopicSuggestionService.normalize(topicName),
            topicName,
            "USER_GENERATED",
            dimension,
            curriculumGroup == null ? "OUT_OF_CURRICULUM" : curriculumGroup,
            true,
            now,
            null
        ));
        topicSuggestionRepository.save(new TopicSuggestion(
            null,
            studentId,
            keyword,
            keyword,
            "UNKNOWN",
            null,
            BigDecimal.ZERO,
            "CREATED",
            "{}",
            "REQUESTED",
            now,
            null
        ));
        return saved.id();
    }
}
