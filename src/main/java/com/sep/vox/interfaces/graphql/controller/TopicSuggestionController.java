package com.sep.vox.interfaces.graphql.controller;

import static com.sep.vox.application.response.input.topicsuggestion.TopicSuggestionResponses.TopicFromKeywordResult;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.command.GenerateTopicFromKeywordCommand;
import com.sep.vox.application.port.input.usecase.topicsuggestion.GenerateTopicFromKeywordUseCase;

@Controller
public class TopicSuggestionController {

    private final GenerateTopicFromKeywordUseCase generateTopicFromKeywordUseCase;
    private final AsyncTaskExecutor practiceGenerationExecutor;

    public TopicSuggestionController(
            GenerateTopicFromKeywordUseCase generateTopicFromKeywordUseCase,
            @Qualifier("practiceGenerationExecutor") AsyncTaskExecutor practiceGenerationExecutor) {
        this.generateTopicFromKeywordUseCase = generateTopicFromKeywordUseCase;
        this.practiceGenerationExecutor = practiceGenerationExecutor;
    }

   
    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public CompletableFuture<TopicFromKeywordResult> generateTopicFromKeyword(
            @Argument("keyword") String keyword) {
        return CompletableFuture.supplyAsync(
            () -> generateTopicFromKeywordUseCase.execute(
                new GenerateTopicFromKeywordCommand(keyword)
            ),
            practiceGenerationExecutor
        );
    }

}
