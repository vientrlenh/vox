package com.sep.vox.interfaces.graphql.controller;

import static com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.InterestProfile;
import static com.sep.vox.application.response.input.topicsuggestion.TopicSuggestionResponses.TopicFromKeywordResult;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.command.GenerateTopicFromKeywordCommand;
import com.sep.vox.application.port.input.command.RespondToTopicSuggestionCommand;
import com.sep.vox.application.port.input.usecase.topicsuggestion.GenerateTopicFromKeywordUseCase;
import com.sep.vox.application.port.input.usecase.topicsuggestion.RespondToTopicSuggestionUseCase;
import com.sep.vox.application.port.input.usecase.topicsuggestion.ViewPendingTopicSuggestionsUseCase;
import com.sep.vox.application.response.input.topicsuggestion.TopicSuggestionResponses.TopicSuggestion;

@Controller
public class TopicSuggestionController {

    private final RespondToTopicSuggestionUseCase respondToTopicSuggestionUseCase;
    private final GenerateTopicFromKeywordUseCase generateTopicFromKeywordUseCase;
    private final ViewPendingTopicSuggestionsUseCase viewPendingTopicSuggestionsUseCase;

    public TopicSuggestionController(
            RespondToTopicSuggestionUseCase respondToTopicSuggestionUseCase,
            GenerateTopicFromKeywordUseCase generateTopicFromKeywordUseCase,
            ViewPendingTopicSuggestionsUseCase viewPendingTopicSuggestionsUseCase) {
        this.respondToTopicSuggestionUseCase = respondToTopicSuggestionUseCase;
        this.generateTopicFromKeywordUseCase = generateTopicFromKeywordUseCase;
        this.viewPendingTopicSuggestionsUseCase = viewPendingTopicSuggestionsUseCase;
    }

    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public boolean respondToTopicSuggestion(
            @Argument("suggestionId") UUID suggestionId,
            @Argument("accept") boolean accept) {
        return respondToTopicSuggestionUseCase.execute(
            new RespondToTopicSuggestionCommand(suggestionId, accept)
        );
    }

    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public TopicFromKeywordResult generateTopicFromKeyword(@Argument("keyword") String keyword) {
        return generateTopicFromKeywordUseCase.execute(new GenerateTopicFromKeywordCommand(keyword));
    }

    @SchemaMapping(typeName = "InterestProfile", field = "suggestions")
    @PreAuthorize("hasRole('STUDENT')")
    public List<TopicSuggestion> suggestions(InterestProfile ignored) {
        return viewPendingTopicSuggestionsUseCase.execute(null);
    }
}
