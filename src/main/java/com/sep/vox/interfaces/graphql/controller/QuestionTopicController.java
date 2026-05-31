package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewQuestionBankDetailsQuery;
import com.sep.vox.application.port.input.query.ViewQuestionTopicDetailsQuery;
import com.sep.vox.application.port.input.query.ViewQuestionTopicsQuery;
import com.sep.vox.application.port.input.usecase.questionbank.ViewQuestionBankDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.ViewQuestionTopicDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.ViewQuestionTopicsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.dto.QuestionTopicDto;

@Controller("graphqlQuestionTopicController")
public class QuestionTopicController {

    private final ViewQuestionTopicsUseCase viewQuestionTopicsUseCase;
    private final ViewQuestionTopicDetailsUseCase viewQuestionTopicDetailsUseCase;
    private final ViewQuestionBankDetailsUseCase viewQuestionBankDetailsUseCase;

    public QuestionTopicController(
            ViewQuestionTopicsUseCase viewQuestionTopicsUseCase,
            ViewQuestionTopicDetailsUseCase viewQuestionTopicDetailsUseCase,
            ViewQuestionBankDetailsUseCase viewQuestionBankDetailsUseCase) {
        this.viewQuestionTopicsUseCase = viewQuestionTopicsUseCase;
        this.viewQuestionTopicDetailsUseCase = viewQuestionTopicDetailsUseCase;
        this.viewQuestionBankDetailsUseCase = viewQuestionBankDetailsUseCase;
    }

    @QueryMapping(name = "questionTopics")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public PageResult<QuestionTopicDto> questionTopics(
            @Argument(name = "bankId") UUID bankId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
        var query = new ViewQuestionTopicsQuery(bankId, page, size);
        return viewQuestionTopicsUseCase.execute(query);
    }

    @QueryMapping(name = "questionTopic")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public QuestionTopicDto questionTopic(@Argument(name = "id") UUID id) {
        var query = new ViewQuestionTopicDetailsQuery(id);
        return viewQuestionTopicDetailsUseCase.execute(query);
    }

    @SchemaMapping(typeName = "QuestionTopic", field = "bank")
    public QuestionBankDto bank(QuestionTopicDto questionTopic) {
        var query = new ViewQuestionBankDetailsQuery(questionTopic.bankId());
        return viewQuestionBankDetailsUseCase.execute(query);
    }
}
