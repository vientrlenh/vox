package com.sep.vox.application.port.input.usecase.practiceplanning;

import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses;
import java.util.List;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.practiceplanning.SearchPracticeTopicsQuery;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeTopicQueryRepository;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.TopicSearchResult;

@Service
@RequiredArgsConstructor
public class SearchPracticeTopicsUseCase implements IUseCase<SearchPracticeTopicsQuery, TopicSearchResult> {

    private final PracticeTopicQueryRepository practiceTopicQueryRepository;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public TopicSearchResult execute(SearchPracticeTopicsQuery input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        String normalized = input.keyword() == null ? "" : input.keyword().strip().toLowerCase();
        if (normalized.isBlank()) {
            return new TopicSearchResult(List.of(), false);
        }
        int minutes = enrichmentService.minutesForStudent(studentId);
        List<PracticePlanningResponses.PracticeTopicOffer> topics = practiceTopicQueryRepository
            .searchTopics(studentId, "%" + normalized + "%", normalized)
            .stream()
            .map(row -> new PracticeTopicOffer(
                row.getId(),
                row.getName(),
                row.getInterestDimension(),
                row.getSavedByMe(),
                null,
                minutes,
                null,
                List.of()
            ))
            .toList();
        // LUÔN cho nhờ AI soạn, không chỉ khi tìm không ra.
        //
        // Bản cũ là `topics.isEmpty()`: tìm ra 3 chủ đề GẦN GIỐNG là nút biến mất, tức bắt học
        // sinh chấp nhận thứ gần đúng. Mà hạn mức 3 lượt/tuần đã gỡ (2026-08-07) nên cũng không
        // còn lý do tiết kiệm nào để giữ chốt này.
        //
        // Không sợ sinh trùng: đã có hai lớp chặn phía sau -- tokenSimilarity >= 0.90 bên Java
        // và TopicDedupeNode bên Chroma. Nhờ soạn thứ đã tồn tại thì trả về chủ đề cũ
        // (MATCHED_EXISTING), không tạo bản sao.
        return new TopicSearchResult(topics, true);
    }
}
