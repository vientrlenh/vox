package com.sep.vox.application.port.input.usecase.practiceplanning;

import com.sep.vox.application.query.dto.TopicSearchRowInfo;
import java.util.Comparator;
import java.util.List;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.practiceplanning.SearchPracticeTopicsQuery;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.TopicGenerationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeTopicQueryRepository;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;


@Service
@RequiredArgsConstructor
public class SearchPracticeTopicsSemanticUseCase
        implements IUseCase<SearchPracticeTopicsQuery, List<PracticeTopicOffer>> {

    private static final int SEMANTIC_RESULT_LIMIT = 5;
    private static final int VECTOR_FETCH_LIMIT = 15;

    private final PracticeTopicQueryRepository practiceTopicQueryRepository;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final TopicGenerationPort topicGenerationPort;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public List<PracticeTopicOffer> execute(SearchPracticeTopicsQuery input) {
        String keyword = input.keyword() == null ? "" : input.keyword().strip();
        if (keyword.isBlank()) {
            return List.of();
        }
        List<TopicGenerationPort.TopicSearchHit> hits = topicGenerationPort.searchByVector(keyword, VECTOR_FETCH_LIMIT);
        if (hits.isEmpty()) {
            return List.of();
        }

        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        // HYDRATE từ Postgres. Không dùng tên trong vector store: đó là bản chụp lúc index, tên
        // có thể đã đổi và chủ đề có thể đã bị tắt. Query cũng lọc luôn `active = true`.
        List<TopicSearchRowInfo> rows = practiceTopicQueryRepository.findActiveByIds(
            studentId,
            hits.stream().map(hit -> hit.topicId()).toList()
        );

        // Giữ THỨ TỰ theo độ tương đồng, không theo thứ tự Postgres trả về -- xếp hạng là toàn
        // bộ giá trị của đường này.
        Map<UUID, Double> rankByTopicId = new java.util.HashMap<java.util.UUID, Double>();
        for (TopicGenerationPort.TopicSearchHit hit : hits) {
            rankByTopicId.put(hit.topicId(), hit.similarity());
        }
        int minutes = enrichmentService.minutesForStudent(studentId);
        return rows.stream()
            .sorted(Comparator.comparingDouble(
                (com.sep.vox.application.query.dto.TopicSearchRowInfo row) ->
                    -rankByTopicId.getOrDefault(row.getId(), 0.0)
            ))
            .map(row -> new PracticeTopicOffer(
                row.getId(),
                row.getName(),
                row.getInterestDimension(),
                row.getSavedByMe(),
                // matchPercent = độ tương đồng ngữ nghĩa, để client hiện nhãn "gần nghĩa" và
                // người dùng hiểu vì sao dòng này xuất hiện dù tên không chứa từ khoá.
                (int) Math.round(rankByTopicId.getOrDefault(row.getId(), 0.0) * 100),
                minutes,
                null,
                List.of()
            ))
            // Cắt SAU khi đã xếp theo độ tương đồng -- cắt trước là vứt mất chính những chủ đề
            // gần nhất chỉ vì Postgres trả chúng về sau.
            .limit(SEMANTIC_RESULT_LIMIT)
            .toList();
    }
}
