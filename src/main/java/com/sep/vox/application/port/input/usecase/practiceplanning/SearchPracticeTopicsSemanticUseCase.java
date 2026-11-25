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
        // HYDRATE from Postgres. The name in the vector store is not used: it is a snapshot taken at
        // index time, the name may have changed and the topic may have been deactivated. The query
        // also filters on `active = true`.
        List<TopicSearchRowInfo> rows = practiceTopicQueryRepository.findActiveByIds(
            studentId,
            hits.stream().map(hit -> hit.topicId()).toList()
        );
        return rankBySimilarity(rows, hits, studentId);
    }

    private List<PracticeTopicOffer> rankBySimilarity(List<TopicSearchRowInfo> rows,
                                                      List<TopicGenerationPort.TopicSearchHit> hits,
                                                      UUID studentId) {
        // Keep the ORDER by similarity, not the order Postgres returned the rows in -- the ranking
        // is the whole value of this path.
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
            .map(row -> toOffer(row, rankByTopicId.getOrDefault(row.getId(), 0.0), minutes))
            // Cut AFTER sorting by similarity -- cutting first would drop the closest topics
            // just because Postgres returned them later.
            .limit(SEMANTIC_RESULT_LIMIT)
            .toList();
    }

    private PracticeTopicOffer toOffer(TopicSearchRowInfo row, double similarity, int minutes) {
        return new PracticeTopicOffer(
            row.getId(),
            row.getName(),
            row.getInterestDimension(),
            row.getSavedByMe(),
            // matchPercent = semantic similarity, so the client can show a "similar meaning" label and
            // the user understands why this row appears even though its name lacks the keyword.
            (int) Math.round(similarity * 100),
            minutes,
            null,
            List.of()
        );
    }
}
