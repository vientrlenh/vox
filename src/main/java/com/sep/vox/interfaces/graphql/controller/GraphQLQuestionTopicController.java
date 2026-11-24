package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.questiontopic.ViewQuestionTopicDetailsQuery;
import com.sep.vox.application.port.input.query.questiontopic.ViewQuestionTopicsQuery;
import com.sep.vox.application.port.input.usecase.questiontopic.ViewQuestionTopicDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.ViewQuestionTopicsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.interfaces.shared.PageArguments;

@Controller
@RequiredArgsConstructor
public class GraphQLQuestionTopicController {

    private final ViewQuestionTopicsUseCase viewQuestionTopicsUseCase;
    private final ViewQuestionTopicDetailsUseCase viewQuestionTopicDetailsUseCase;

    @QueryMapping(name = "questionTopics")
    public PageResult<QuestionTopicDto> questionTopics(
            @Argument(name = "questionBankId") UUID questionBankId,
            @Argument(name = "status") QuestionTopicStatus status,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        ViewQuestionTopicsQuery query = new ViewQuestionTopicsQuery(questionBankId, status, keyword, page, size);
        return viewQuestionTopicsUseCase.execute(query);
    }

    @QueryMapping(name = "questionTopic")
    public QuestionTopicDto questionTopic(@Argument(name = "id") UUID id) {
        ViewQuestionTopicDetailsQuery query = new ViewQuestionTopicDetailsQuery(id);
        return viewQuestionTopicDetailsUseCase.execute(query);
    }
}
