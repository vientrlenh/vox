package com.sep.vox.application.port.input.usecase.practiceplanning;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.SearchPracticeTopicsQuery;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeTopicQueryRepository;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.TopicSearchResult;

@Service
public class SearchPracticeTopicsUseCase implements IUseCase<SearchPracticeTopicsQuery, TopicSearchResult> {

    private final PracticeTopicQueryRepository practiceTopicQueryRepository;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final UserContextPort userContextPort;

    public SearchPracticeTopicsUseCase(
            PracticeTopicQueryRepository practiceTopicQueryRepository,
            PracticeTopicOfferEnrichmentService enrichmentService,
            UserContextPort userContextPort) {
        this.practiceTopicQueryRepository = practiceTopicQueryRepository;
        this.enrichmentService = enrichmentService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public TopicSearchResult execute(SearchPracticeTopicsQuery input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var normalized = input.keyword() == null ? "" : input.keyword().strip().toLowerCase();
        if (normalized.isBlank()) {
            return new TopicSearchResult(List.of(), false);
        }
        var minutes = enrichmentService.minutesForStudent(studentId);
        var topics = practiceTopicQueryRepository
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
        return new TopicSearchResult(topics, topics.isEmpty());
    }
}
