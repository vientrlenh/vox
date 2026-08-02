package com.sep.vox.application.port.input.usecase.topicsuggestion;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.topicsuggestion.TopicSuggestionResponses.TopicSuggestion;
import com.sep.vox.application.port.input.service.TopicSuggestionService;

@Service
public class ViewPendingTopicSuggestionsUseCase implements IUseCase<Void, List<TopicSuggestion>> {

    private final TopicSuggestionService topicSuggestionRepository;
    private final UserContextPort userContextPort;

    public ViewPendingTopicSuggestionsUseCase(
            TopicSuggestionService topicSuggestionRepository,
            UserContextPort userContextPort) {
        this.topicSuggestionRepository = topicSuggestionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicSuggestion> execute(Void input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        return topicSuggestionRepository.pendingSuggestions(studentId);
    }
}
