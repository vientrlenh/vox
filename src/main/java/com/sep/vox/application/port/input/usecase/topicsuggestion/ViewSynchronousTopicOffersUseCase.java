package com.sep.vox.application.port.input.usecase.topicsuggestion;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewSynchronousTopicOffersQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;
import com.sep.vox.application.port.input.service.TopicSuggestionService;

@Service
public class ViewSynchronousTopicOffersUseCase implements IUseCase<ViewSynchronousTopicOffersQuery, List<PracticeTopicOffer>> {

    private final TopicSuggestionService topicSuggestionRepository;
    private final UserContextPort userContextPort;

    public ViewSynchronousTopicOffersUseCase(
            TopicSuggestionService topicSuggestionRepository,
            UserContextPort userContextPort) {
        this.topicSuggestionRepository = topicSuggestionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public List<PracticeTopicOffer> execute(ViewSynchronousTopicOffersQuery input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        return topicSuggestionRepository.synchronousOffers(studentId, input.requestedCount());
    }
}
