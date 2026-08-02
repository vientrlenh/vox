package com.sep.vox.application.port.input.usecase.topicsuggestion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.GenerateTopicFromKeywordCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.topicsuggestion.TopicSuggestionResponses.TopicFromKeywordResult;
import com.sep.vox.application.port.input.service.TopicSuggestionService;

@Service
public class GenerateTopicFromKeywordUseCase implements IUseCase<GenerateTopicFromKeywordCommand, TopicFromKeywordResult> {

    private final TopicSuggestionService topicSuggestionRepository;
    private final UserContextPort userContextPort;

    public GenerateTopicFromKeywordUseCase(
            TopicSuggestionService topicSuggestionRepository,
            UserContextPort userContextPort) {
        this.topicSuggestionRepository = topicSuggestionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public TopicFromKeywordResult execute(GenerateTopicFromKeywordCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        return topicSuggestionRepository.generateFromKeyword(studentId, input.keyword());
    }
}
