package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewQuestionTopicDetailsQuery;
import com.sep.vox.application.port.input.query.ViewQuestionTopicsQuery;
import com.sep.vox.application.port.input.usecase.questiontopic.ViewQuestionTopicDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.ViewQuestionTopicsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.model.question.QuestionTopicStatus;

@Controller("graphqlQuestionTopicController")
public class QuestionTopicController {

    private final ViewQuestionTopicsUseCase viewQuestionTopicsUseCase;
    private final ViewQuestionTopicDetailsUseCase viewQuestionTopicDetailsUseCase;

    public QuestionTopicController(
            ViewQuestionTopicsUseCase viewQuestionTopicsUseCase,
            ViewQuestionTopicDetailsUseCase viewQuestionTopicDetailsUseCase) {
        this.viewQuestionTopicsUseCase = viewQuestionTopicsUseCase;
        this.viewQuestionTopicDetailsUseCase = viewQuestionTopicDetailsUseCase;
    }

    @QueryMapping(name = "questionTopics")
    public PageResult<QuestionTopicDto> questionTopics(
            @Argument(name = "questionBankId") UUID questionBankId,
            @Argument(name = "status") QuestionTopicStatus status,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        validatePage(page, size);
        var query = new ViewQuestionTopicsQuery(questionBankId, status, keyword, page, size);
        return viewQuestionTopicsUseCase.execute(query);
    }

    @QueryMapping(name = "questionTopic")
    public QuestionTopicDto questionTopic(@Argument(name = "id") UUID id) {
        var query = new ViewQuestionTopicDetailsQuery(id);
        return viewQuestionTopicDetailsUseCase.execute(query);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
    }
}
