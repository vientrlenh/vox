package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.model.personalization.TopicSuggestion;
import com.sep.vox.domain.repository.PracticeTopicRepository;
import com.sep.vox.domain.repository.TopicSuggestionRepository;

/**
 * Ghi topic mới sinh từ từ khoá + ghi dòng nhật ký yêu cầu -- HAI lệnh ghi GẮN VỚI NHAU về
 * nghiệp vụ, nên phải cùng thành công hoặc cùng huỷ: topic tồn tại mà không có dòng nhật ký
 * nào trỏ tới thì sau này không truy được nó ra đời từ từ khoá nào của ai.
 *
 * Từ 2026-08-07 dòng nhật ký này KHÔNG còn chi phối gì (hạn mức 3 lượt/tuần đã gỡ, tìm bằng
 * AI không giới hạn số lần) -- nhưng tính nguyên tử vẫn giữ, vì lý do truy vết ở trên.
 *
 * Là bean riêng, KHÔNG phải method trong TopicSuggestionService, vì:
 * (a) self-invocation bỏ qua proxy AOP nên @Transactional sẽ không có tác dụng;
 * (b) generateFromKeyword có gọi LLM chậm ở giữa, không được nằm trong transaction.
 */
@Service
@RequiredArgsConstructor
public class KeywordTopicPersistenceService {

    private final PracticeTopicRepository practiceTopicRepository;
    private final TopicSuggestionRepository topicSuggestionRepository;

    @Transactional
    public UUID createTopicAndRecordRequest(
            UUID studentId,
            String keyword,
            String topicName,
            String dimension,
            String curriculumGroup,
            String temporalAffordance) {
        Instant now = Instant.now();
        PracticeTopic saved = practiceTopicRepository.save(PracticeTopic.generatedFromName(
                "USER_GENERATED",
                topicName,
                TopicSuggestionService.normalize(topicName),
                dimension,
                curriculumGroup,
                temporalAffordance,
                now));
        topicSuggestionRepository.save(TopicSuggestion.keywordRequested(studentId, keyword, "CREATED", now));
        return saved.getId();
    }
}
