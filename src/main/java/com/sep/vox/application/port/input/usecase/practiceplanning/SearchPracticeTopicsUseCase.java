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
        // ALWAYS offer AI drafting, not only when nothing was found.
        //
        // The old condition was `topics.isEmpty()`: finding 3 SIMILAR topics made the button disappear,
        // forcing the student to settle for an approximation. The weekly limit of 3 requests was
        // removed (2026-08-07), so there is no saving left that would justify keeping that gate.
        //
        // Duplicates are not a worry: two layers block them downstream -- tokenSimilarity >= 0.90 on
        // the Java side and TopicDedupeNode on the Chroma side. Asking to draft something that already
        // exists returns the old topic (MATCHED_EXISTING), no copy is created.
        return new TopicSearchResult(findMatchingOffers(studentId, normalized), true);
    }

    private List<PracticePlanningResponses.PracticeTopicOffer> findMatchingOffers(UUID studentId, String normalized) {
        int minutes = enrichmentService.minutesForStudent(studentId);
        return practiceTopicQueryRepository
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
    }
}
