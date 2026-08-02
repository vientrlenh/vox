package com.sep.vox.application.port.input.usecase.topicsuggestion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.RespondToTopicSuggestionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.port.input.service.TopicSuggestionService;

@Service
public class RespondToTopicSuggestionUseCase implements IUseCase<RespondToTopicSuggestionCommand, Boolean> {

    private final TopicSuggestionService topicSuggestionRepository;
    private final UserContextPort userContextPort;

    public RespondToTopicSuggestionUseCase(
            TopicSuggestionService topicSuggestionRepository,
            UserContextPort userContextPort) {
        this.topicSuggestionRepository = topicSuggestionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Boolean execute(RespondToTopicSuggestionCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        return topicSuggestionRepository.respond(studentId, input.suggestionId(), input.accept());
    }
}
