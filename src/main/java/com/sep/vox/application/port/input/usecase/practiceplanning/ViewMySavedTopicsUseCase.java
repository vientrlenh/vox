package com.sep.vox.application.port.input.usecase.practiceplanning;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeTopicQueryRepository;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;

@Service
public class ViewMySavedTopicsUseCase implements IUseCase<Void, List<PracticeTopicOffer>> {

    private final PracticeTopicQueryRepository practiceTopicQueryRepository;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final UserContextPort userContextPort;

    public ViewMySavedTopicsUseCase(
            PracticeTopicQueryRepository practiceTopicQueryRepository,
            PracticeTopicOfferEnrichmentService enrichmentService,
            UserContextPort userContextPort) {
        this.practiceTopicQueryRepository = practiceTopicQueryRepository;
        this.enrichmentService = enrichmentService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticeTopicOffer> execute(Void input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var minutes = enrichmentService.minutesForStudent(studentId);
        var focusTags = enrichmentService.focusTagsForStudent(studentId);
        return practiceTopicQueryRepository.findSavedTopics(studentId).stream()
            .map(row -> new PracticeTopicOffer(
                row.getId(),
                row.getName(),
                row.getInterestDimension(),
                row.getSavedByMe(),
                null,
                minutes,
                null,
                List.of(),
                focusTags
            ))
            .toList();
    }
}
